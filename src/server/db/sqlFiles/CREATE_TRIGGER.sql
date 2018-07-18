CREATE TRIGGER my_trigger AFTER INSERT
ON USERS
BEGIN
    UPDATE BOOK
    SET FRIEND_ID = NEW.USER_ID
    WHERE FRIEND_PHONE = NEW.PHONE
    ;
END
;;
---------------------------------------------------------------------
CREATE TRIGGER my_trigger2 AFTER UPDATE
ON USERS
BEGIN
    UPDATE BOOK
    SET FRIEND_PHONE = NEW.PHONE
    WHERE FRIEND_ID = NEW.USER_ID
    ;
END
;;
---------------------------------------------------------------------
CREATE TRIGGER my_trigger3 AFTER DELETE
ON USERS
BEGIN
    UPDATE BOOK
    SET FRIEND_ID = NULL
    WHERE FRIEND_ID = OLD.USER_ID
    ;
END
;;