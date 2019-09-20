update payment_cycle set cycle_end_date = cycle_end_date - INTERVAL '1 DAY'
FROM claim WHERE claim_id = claim.id and claim.claim_status in ('ACTIVE', 'PENDING_EXPIRY')
and cycle_end_date > now() and cycle_end_date < '2019-10-19';