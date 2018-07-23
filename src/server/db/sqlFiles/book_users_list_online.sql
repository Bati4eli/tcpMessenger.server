select
    DISTINCT USER_ID
from BOOK
where FRIEND_ID = @USER_ID@
;