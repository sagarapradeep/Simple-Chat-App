CREATE TABLE IF NOT EXISTS SignUpUsers
(
    username VARCHAR(100) primary key,
    name     VARCHAR(200) NOT NULL ,
    gender   ENUM ('Male','Female') NOT NULL ,
    country  VARCHAR(100) NOT NULL ,
    birthday DATE NOT NULL ,
    password VARCHAR(300) NOT NULL


);








