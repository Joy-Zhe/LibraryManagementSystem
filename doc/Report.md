# 实验5 图书管理系统
### 郑乔尹 3210104169
# 实验目的
1. 设计并实现一个精简的图书管理系统，具有**入库**、**查询**、**借书**、**还书**、**借书证管理**等基本功能
2. 通过本次设计来加深对数据库的了解和使用，同时提高自身的系统编程能力
# 实验平台
1. 数据库平台：SQL Server 2022
2. 操作系统：Windows11 22H2
3. 开发工具：JetBrain IntelliJ IDEA
# 实验内容和要求
1. 基于JDBC开发一个图书管理系统，要求实现如下功能

|功能|描述|
|:----:|:----:|
|图书入库|输入<书号, 类别, 书名, 出版社, 年份, 作者, 价格, 初始库存>，入库一本新书B
|增加库存|将书B的库存增加到X，然后减少到1
|修改图书信息|随机抽取N个字段，修改图书B的图书信息
|批量入库|输入图书导入文件的路径U，然后从文件U中批量导入图书
|添加借书证|输入<姓名, 单位, 身份>，添加一张新的借书证C
|查询借书证|列出所有的借书证
|借书|用借书证C借图书B，再借一次B，然后再借一本书K
|还书|用借书证C还掉刚刚借到的书B
|借书记录查询|查询C的借书记录
|图书查询|从查询条件<类别点查(精确查询)，书名点查(模糊查询)，出版社点查(模糊查询)，年份范围查，作者点查(模糊查询)，价格范围差>中随机选取N个条件，并随机选取一个排序列和顺序
2. 以下是数据表的定义
```SQL
create table `book` (
    `book_id` int not null auto_increment,
    `category` varchar(63) not null,
    `title` varchar(63) not null,
    `press` varchar(63) not null,
    `publish_year` int not null,
    `author` varchar(63) not null,
    `price` decimal(7, 2) not null default 0.00,
    `stock` int not null default 0,
    primary key (`book_id`),
    unique (`category`, `press`, `author`, `title`, `publish_year`)
);

create table `card` (
    `card_id` int not null auto_increment,
    `name` varchar(63) not null,
    `department` varchar(63) not null,
    `type` char(1) not null,
    primary key (`card_id`),
    unique (`department`, `type`, `name`),
    check ( `type` in ('T', 'S') )
);

create table `borrow` (
  `card_id` int not null,
  `book_id` int not null,
  `borrow_time` bigint not null,
  `return_time` bigint not null default 0,
  primary key (`card_id`, `book_id`, `borrow_time`),
  foreign key (`card_id`) references `card`(`card_id`) on delete cascade on update cascade,
  foreign key (`book_id`) references `book`(`book_id`) on delete cascade on update cascade
);
```

# 实验过程
### 1. 单本图书入库
#### 功能要求
+ 对单本图书进行入库，要求实现查重功能，对category, title, press， publish year, author三个字段进行查重操作，即对于这五个字段相同的书籍，视为重复书籍，不进行入库
+ 对于不重复的书籍，分配book id，入库
#### 原理
+ 采用如下SQL语句
+ 由于book id设为自动分配的主键，会自动生成当前最后一个连续值
##### 书籍查重
```SQL
SELECT * FROM book WHERE category = ? AND title = ? AND press = ? AND publish_year = ? AND author = 
```
##### 插入书籍
```SQL
INSERT INTO book (category, title, press, publish_year, author, price, stock) VALUES (?, ?, ?, ?, ?, ?, ?)
```
### 2. 减少库存
#### 功能要求
+ 对于输入的两个参数`bookID`, `deltaStock`，先对书库中的书籍进行查询，如果书籍不存在，则减少库存失败；若书籍存在，需要先对书籍现在的库存进行检测，若书籍的库存不足以减少这么多书籍，则减少库存失败，反之更新库存。
#### 原理
##### 书籍存在性检测
```SQL
SELECT * FROM book WHERE book_id = ?
```
##### 书籍库存检查
+ 在上述查询出来的结果集中，对列`stock`进行查询
+ 检查其剩余库存
```Java
int currentStock = resultSet.getInt("stock");
if (currentStock + deltaStock < 0) {
	rollback(connection);
	return new ApiResult(false, "No such books to be incline!");
} else {
	rollback(connection);
	return new ApiResult(false, "No such book_id!");
}
catch (SQLException e) {
	throw new RuntimeException(e);
}
```
##### 减少库存
```SQL
UPDATE book SET stock = stock + ? WHERE book_id = ?
```

### 3. 图书批量入库
#### 要求
+ 一次加入多本书籍，同时，对这些插入的书籍也进行和单本一样的查重，一旦有重复，则插入失败，整个List无重复则插入成功
#### 原理
+ 对书籍进行查重以及插入的SQL语句
```SQL
SELECT * FROM book WHERE category = ? AND title = ? AND press = ? AND publish_year = ? AND author = ?
INSERT INTO book (category, title, press, publish_year, author, price, stock) VALUES (?, ?, ?, ?, ?, ?, ?)
```
+ 对列表中的书籍，使用循环进行插入，每插入一本，使用查重语句进行查询，若查询结果集非空，则可以返回false，终止插入，回滚更改，否则继续处理下一条数据。
+ 若所有数据均无重复，则实现正确插入


### 4. 移除书籍
#### 要求
+ 从书库中移除书籍，要求移除的书籍必须是已经归还的
#### 原理
##### 查询书籍存在性以及归还状态
```SQL
SELECT * FROM borrow WHERE book_id = ? and return_time = 0
```
+ 该SQL语句查询了书库中未归还的需移除书籍，若结果集非空，则归还失败
##### 移除书籍
```SQL
DELETE FROM book WHERE book_id = ?
```


### 5. 更改图书信息
#### 要求
+ 对书库中的对应书籍进行信息修改
#### 原理
+ 查询和更新同时进行，使用一条语句，结果集非空则可更新
```SQL
update book set category = ? , title = ? , press = ? , publish_year = ? , author = ? , price = ? WHERE book_id = ?
```

### 6. 书籍查询
#### 要求
+ 对于输入的查询条件，对书库中的书籍进行查询，支持类别点查(精确查询)，书名点查(模糊查询)，出版社点查(模糊查询)，年份范围查，作者点查(模糊查询)，价格范围差，同时指定一个排序方式，默认为`book id`升序，返回查询结果。
#### 原理
+ 使用`StringBuilder`，简化对SQL语句条件的添加，并利用`List`对相应追加条件的待定值`?`进行保存和赋值：
```Java
StringBuilder queryBuilder = new StringBuilder("select * from book where 1=1"); // 1=1 占位   
List<Object> queryParams = new ArrayList<>();  
  
if(conditions.getCategory() != null){  
	queryBuilder.append(" AND category = ?"); //追加条件  
	queryParams.add(conditions.getCategory());  
}
```
+ 模糊查询，使用`LIKE`：
```SQL
SELECT * FROM book WHERE title LIKE ? 
```
> ? 将被替换为形如%String%的形式，%代表多个或没有字符


### 7. 借书
#### 要求
+ 对于给定的借书人和借阅书籍以及借阅时间，在书库中查找书籍，若书籍尚有库存，且该借书人不存在借此书不还的情况，则借书成功，生成借书记录，更新表`borrow`
+ 存在批量操作，多个线程对库存同时出现影响的情况，需要解决这种情况
#### 原理
##### 库存更新锁
+ 通过加锁，来实现一个线程对一本书籍进行库存操作时，其他线程无法对相同的书籍库存进行更改，从而避免了并发借书的问题
+ SQL语句如下，添加了行级锁：
```SQL
SELECT * FROM book WITH (UPDLOCK, ROWLOCK) WHERE book_id = ? AND stock > 0
```
##### 插入借书记录
```SQL
INSERT INTO borrow (card_id, book_id, borrow_time, return_time) values (?, ?, ?, ?)
```
##### 更新库存
``` SQL
UPDATE book SET stock = stock - 1 WHERE book_id = ?
```

### 8. 还书
#### 要求
+ 对于当前未还的存在书籍，执行还书操作，将库存加一
#### 原理
+ 查询是否存在以及当前是否存在未还记录
```SQL
SELECT * FROM borrow WHERE card_id = ? AND book_id = ? AND return_time = 0
```
+ 若存在未还记录，更新该书库存
```SQL
UPDATE book SET stock = stock + 1 WHERE book_id = ?
```

### 9. 查询用户历史记录
#### 要求
+ 对给定的用户`Card ID`，显示其全部借阅记录，要求包含书籍信息并按照借阅时间**降序**、`book id`**升序**排序
#### 原理
+ 先通过`borrow`表查询`book id`，再通过此`book id`在`book`表中查询书籍信息
1. 查询`book id`
```SQL
SELECT * FROM borrow WHERE card_id = ? order by borrow_time DESC, book_id ASC
```
2. 查询书籍信息
```SQL
SELECT * FROM book WHERE book_id = ?
```

### 10. 注册用户卡
#### 要求
+ 给出用户的姓名、部门、用户种类($S$, $T$)信息，生成一个`card id`，需要查重
#### 原理
+ 查重，新建卡，自动生成`card id`
```SQL
select * from card where name = ? and department = ? and type = ?
insert into card (name, department, type) values (?, ?, ?)
```

### 11. 删除用户卡
#### 要求
+ 对于存在的用户卡，若不存在未归还书籍，则可对其用户卡进行删除
#### 原理
+ 查询对应`card id`的借书记录中，是否有未还书籍
```SQL
SELECT * FROM borrow WHERE card_id = ? AND return_time = 0
```
+ 若无未归还书籍，移除卡
```SQL
DELETE FROM card WHERE card_id = ?
```

### 12. 展示所有用户卡
#### 要求
+ 以`card id`升序展示所有用户卡
#### 原理
+ 查询语句
```SQL 
SELECT * FROM card order by ASC
```

### GUI 
#### 基于Java Swing
##### JFrame
##### JButton
##### JLabel
# 实验成果
### GUI
+ 图书相关操作
![](pic/book.png)

+ 借书相关操作
![](pic/borrow.png)

+ 借书卡相关操作
![](card.png)
