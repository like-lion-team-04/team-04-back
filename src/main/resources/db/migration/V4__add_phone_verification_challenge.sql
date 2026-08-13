alter table phone_verifications add column challenge_encrypted varchar(512);

-- Existing NCP verification rows cannot be confirmed through OCTOMO and are intentionally invalidated.
update phone_verifications
set challenge_encrypted = phone_encrypted,
    status = case when status = 'PENDING' then 'LOCKED' else status end
where challenge_encrypted is null;

alter table phone_verifications alter column challenge_encrypted set not null;
