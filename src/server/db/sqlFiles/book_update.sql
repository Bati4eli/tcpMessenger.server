UPDATE BOOK
SET FULLNAME = '@FULLNAME@'
WHERE USER_ID = @USER_ID@
    AND FRIEND_PHONE = '@FRIEND_PHONE@'
;
