SELECT
    U.*
    ,COALESCE(
            CAST((
                JulianDay(current_timestamp) - JulianDay( MAX(DATE_BIND) )
            ) * 24 * 60 As Integer)
        ,1000) as MINUTES_LAST_BIND
FROM USERS U
LEFT JOIN DEVICES D
    USING(USER_ID)
WHERE U.PHONE = '@PHONE@'
GROUP BY USER_ID
;