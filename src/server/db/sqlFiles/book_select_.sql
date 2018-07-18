SELECT
    B.USER_ID                                     as FRIEND_ID
    , A.FRIEND_PHONE                              as FRIEND_PHONE
    , B.NICKNAME                                  as NICKNAME
    , A.FULLNAME                                  as FULLNAME
    , WAS_IN_BASE                                 as WAS_IN_BASE
    , IS_UPDATE                                   as IS_UPDATE
    , CASE WHEN B.PHONE IS NULL THEN 0 ELSE 1 END     as IS_REGISTERED
    , CASE WHEN F.FRIEND_ID IS NULL THEN 0 ELSE 1 END as IS_FRIEND
FROM (
        SELECT
            COALESCE(A_FPN,D_FPN)   as FRIEND_PHONE
            , COALESCE(A_NAME,D_NAME )  as FULLNAME
            , CASE WHEN NOT D_FPN IS NULL AND A_FPN IS NULL THEN 'SERVER_HAVE'
                   WHEN D_FPN IS NULL AND NOT A_FPN IS NULL THEN 'CLIENT_HAVE'
                   ELSE 'BOTH_HAVE'
              END         as WAS_IN_BASE
            , CASE WHEN A_FPN IS NULL AND D_FPN IS NULL THEN 0 ELSE
                    CASE WHEN A_NAME != D_NAME THEN 1
                    ELSE 0
                    END
                END                                       as IS_UPDATE
        FROM (   /* -- FULL OUTER JOIN */
                SELECT
                    A.FRIEND_PHONE  as A_FPN
                    ,A.FULLNAME     as A_NAME
                    ,D.FRIEND_PHONE as D_FPN
                    ,D.FULLNAME     as D_NAME
                FROM   BOOK D
                LEFT JOIN ( @ITEMS@) A
                    on A.FRIEND_PHONE= D.FRIEND_PHONE
                WHERE D.USER_ID = @USER_ID@

                UNION

                SELECT
                    A.FRIEND_PHONE  as A_FPN
                    ,A.FULLNAME     as A_NAME
                    ,D.FRIEND_PHONE as D_FPN
                    ,D.FULLNAME     as D_NAME
                FROM   ( @ITEMS@) A
                LEFT JOIN BOOK D
                    on A.FRIEND_PHONE = D.FRIEND_PHONE
                        AND D.USER_ID = @USER_ID@
             ) A
     ) A
LEFT JOIN USERS as B
    ON B.PHONE = A.FRIEND_PHONE
LEFT JOIN BOOK as F
    ON  F.USER_ID    = B.USER_ID
    AND F.FRIEND_ID  = @USER_ID@
;
