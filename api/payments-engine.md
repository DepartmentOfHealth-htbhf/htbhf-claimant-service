The Payments Engine
-------------------

The payments engine is the core of the Claimant Service. It is responsible for creating a new payment cycle for every active  claim,
every 28 days; confirming that the claimant is still eligible; calculating the correct payment amount, making the payment
and informing the user.
This is accomplished with a combination of scheduled services and message processors. Note we're not using message driven beans 
or a fully-fledged messaging component such as RabbitMQ - instead messages are persisted in a database table (`message_queue`)
and message processors are triggered on a schedule. This design decision was driven by the lack of a messaging component provided by the GOV.Uk PaaS.
Message processors have been deliberately separated from the scheduling components so that in future another messaging component could be introduced.

Schedulers use [ShedLock](https://github.com/lukas-krecan/ShedLock) to ensure that a given scheduled task can run on only one application instance at a time.
Each message type has its own schedule, so different message types can be processed concurrently, and on different app instances.
Currently messages of the same type are processed sequentially (in batches of 1,000) - though it would be possible to modify the [`MessageProcessor`](src/main/java/uk/gov/dhsc/htbhf/claimant/message/MessageProcessor.java) to use an
ExecutorService and allow parallel processing of multiple messages of the same type (on a single app instance).
The number of scheduled tasks that can be run concurrently on a single instance is governed by the `spring.task.scheduling.pool.size` property in [application.yml](src/main/resources/application.yml).

## Scheduled Services

All scheduled services are in the `uk.gov.dhsc.htbhf.claimant.scheduler` package.
Schedules are defined in [application.yml](src/main/resources/application.yml), using the [Spring variant of the Cron format](https://docs.spring.io/spring/docs/current/javadoc-api/org/springframework/scheduling/support/CronSequenceGenerator.html). 

#### [`PaymentCycleScheduler`](src/main/java/uk/gov/dhsc/htbhf/claimant/scheduler/PaymentCycleScheduler.java)
This queries the database to identify all active claims with a `PaymentCycle` ending today (or after a configurable number of days, e.g. tomorrow)
then triggers the [`CreateNewPaymentCycleJob`](src/main/java/uk/gov/dhsc/htbhf/claimant/scheduler/CreateNewPaymentCycleJob.java) for each claim
to: create a new payment cycle, put a `DETERMINE_ENTITLEMENT` message on the queue to start the process of completing the payment cycle.

#### [`CardCancellationScheduler`](src/main/java/uk/gov/dhsc/htbhf/claimant/scheduler/CardCancellationScheduler.java)
This queries the database for all claims that have had a card status of `PENDING_CANCELLATION` for 16 weeks or more, then triggers the
[`HandleCardPendingCancellationJob`](src/main/java/uk/gov/dhsc/htbhf/claimant/scheduler/HandleCardPendingCancellationJob.java) to send a notification to the claimant
and update the card status to `SCHEDULED_FOR_CANCELLATION`.

##### *TODO*: There is currently no scheduler to actually cancel the card. 

### [`MessageProcessorScheduler`](src/main/java/uk/gov/dhsc/htbhf/claimant/scheduler/MessageProcessorScheduler.java)
This is where the bulk of the action occurs. Has one schedule for each [`MessageType`](src/main/java/uk/gov/dhsc/htbhf/claimant/message/MessageType.java),
which invoke the [`MessageProcessor`](src/main/java/uk/gov/dhsc/htbhf/claimant/message/MessageProcessor.java) to
query the database for (up to 1,000) messages of the given type and pass each message to the 
[`MessageTypeProcessor`](src/main/java/uk/gov/dhsc/htbhf/claimant/message/MessageTypeProcessor.java) registered to process messages of that type.

Messages are only returned from the database query if their `processAfter` timestamp is in the past.
This allows failed messages to be returned to the message queue with a delay added to their `processAfter` timestamp,
calculated using an [exponential back-off algorithm](src/main/java/uk/gov/dhsc/htbhf/claimant/message/MessageStatusProcessor.java).

