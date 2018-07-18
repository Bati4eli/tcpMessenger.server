SELECT  COUNT(DISTINCT DEVICE_ID) as AMOUNT
from devices
WHERE DATE_BIND >  date('now','-1 day')
    AND PHONE = @USER_ID@
;