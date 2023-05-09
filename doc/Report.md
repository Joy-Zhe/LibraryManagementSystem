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
#### source code
```Java
public ApiResult storeBook(Book book) {  
String checkBook = "SELECT * FROM book WHERE category = ? AND title = ? AND press = ? AND publish_year = ? AND author = ?";  
String insertBook = "INSERT INTO book (category, title, press, publish_year, author, price, stock) VALUES (?, ?, ?, ?, ?, ?, ?)";  
Connection connection = connector.getConn();  
  
try {  
PreparedStatement bookStatement = connection.prepareStatement(checkBook);  
bookStatement.setString(1, book.getCategory());  
bookStatement.setString(2, book.getTitle());  
bookStatement.setString(3, book.getPress());  
bookStatement.setInt(4, book.getPublishYear());  
bookStatement.setString(5, book.getAuthor());  
ResultSet resultSet = bookStatement.executeQuery();  
if(resultSet.next()) { //找到了一样的  
return new ApiResult(false, "Error! Already Exists!");  
}  
} catch (SQLException e) {  
throw new RuntimeException(e);  
}  
  
try {  
PreparedStatement bookStatement = connection.prepareStatement(insertBook, Statement.RETURN_GENERATED_KEYS);// 补全占位符，获取自动主键  
bookStatement.setString(1, book.getCategory());  
bookStatement.setString(2, book.getTitle());  
bookStatement.setString(3, book.getPress());  
bookStatement.setInt(4, book.getPublishYear());  
bookStatement.setString(5, book.getAuthor());  
bookStatement.setDouble(6, book.getPrice());  
bookStatement.setDouble(7, book.getStock());  
int newRow = bookStatement.executeUpdate();  
if (newRow == 0) {  
throw new SQLException("Creating book failed, no rows affected.");  
}  
  
try (ResultSet generatedKeys = bookStatement.getGeneratedKeys()) {  
if (generatedKeys.next()) {  
book.setBookId(generatedKeys.getInt(1));  
} else {  
throw new SQLException("Creating book failed, no ID obtained.");  
}  
}  
commit(connection);  
return new ApiResult(true, "Stored successfully");  
} catch (SQLException e) {  
rollback(connection);  
e.printStackTrace();  
return new ApiResult(false, "Error storing book: " + e.getMessage());  
// throw new RuntimeException(e);  
}  
}
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
#### source code
```Java
public ApiResult incBookStock(int bookId, int deltaStock) {  
	String checkStock = "select * from book where book_id = ?";  
	String inclineStock = "update book set stock = stock + ? where book_id = ?";  
	Connection connection = connector.getConn();  
  
	try {  
		PreparedStatement checkStatement = connection.prepareStatement(checkStock);  
		checkStatement.setInt(1, bookId);  
		ResultSet resultSet = checkStatement.executeQuery();  
		if (resultSet.next()) {  
			int currentStock = resultSet.getInt("stock");  
			if (currentStock + deltaStock < 0) {  
				rollback(connection);  
				return new ApiResult(false, "No such books to be incline!");  
			}  
		} else {  
			rollback(connection);  
			return new ApiResult(false, "No such book_id!");  
		}  
	} catch (SQLException e) {  
		throw new RuntimeException(e);  
	}  
  
	try {  
		PreparedStatement inclineStatement = connection.prepareStatement(inclineStock);
		inclineStatement.setInt(1, deltaStock);  
		inclineStatement.setInt(2, bookId);  
		int newRow = inclineStatement.executeUpdate();  
  
		if (newRow > 0) {  
			commit(connection);  
			return new ApiResult(true, "Incline Successfully!");  
		} else {  
			rollback(connection);  
			return new ApiResult(false, "Incline failed!");  
		}  
	} catch (SQLException e) {  
		rollback(connection);  
		e.printStackTrace();  
		return new ApiResult(false, "Incline failed!");  
	}  
}
```
### 3. 图书批量入库
#### 原理
+ 可能有点问题，一会重写
#### source code
```Java
public ApiResult storeBook(List<Book> books) {  
	String checkBook = "SELECT * FROM book WHERE category = ? AND title = ? AND press = ? AND publish_year = ? AND author = ?";  
	String insertBook = "INSERT INTO book (category, title, press, publish_year, author, price, stock) VALUES (?, ?, ?, ?, ?, ?, ?)";  
	Connection connection = connector.getConn();  
  
	try {  
		for (Book book : books) {  
			PreparedStatement bookStatement = connection.prepareStatement(checkBook);  
			bookStatement.setString(1, book.getCategory());  
			bookStatement.setString(2, book.getTitle());  
			bookStatement.setString(3, book.getPress());  
			bookStatement.setInt(4, book.getPublishYear());  
			bookStatement.setString(5, book.getAuthor());  
			ResultSet resultSet = bookStatement.executeQuery();  
			if (resultSet.next()) { //找到了一样的  
				rollback(connection);  
				return new ApiResult(false, "Error! Already Exists!");  
			}  
			bookStatement = connection.prepareStatement(insertBook, Statement.RETURN_GENERATED_KEYS);  
			bookStatement.setString(1, book.getCategory());  
			bookStatement.setString(2, book.getTitle());  
			bookStatement.setString(3, book.getPress());  
			bookStatement.setInt(4, book.getPublishYear());  
			bookStatement.setString(5, book.getAuthor());  
			bookStatement.setDouble(6, book.getPrice());  
			bookStatement.setDouble(7, book.getStock());  
			int newRow = bookStatement.executeUpdate();  
			if (newRow == 0) {  
				throw new SQLException("Creating book failed, no rows affected.");  
			}  
  
			try (ResultSet generatedKeys = bookStatement.getGeneratedKeys()) {  
				if (generatedKeys.next()) {  
					book.setBookId(generatedKeys.getInt(1));  
				} else {  
					throw new SQLException("Creating book failed, no ID obtained.");  
				}  
			}  
		}  
		commit(connection); // 提交事务  
		return new ApiResult(true, "Stored successfully");  
	} catch (SQLException e) {  
		rollback(connection); // 发生异常时回滚事务  
		e.printStackTrace();  
		return new ApiResult(false, "Error storing book: " + e.getMessage());  
	} finally {  
		try {  
			connection.setAutoCommit(true); // 恢复自动提交  
		} catch (SQLException e) {  
			e.printStackTrace();  
		}  
	}  
}
```

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
#### source code
```Java
public ApiResult removeBook(int bookId) {  
	String checkBook = "select * from borrow where book_id = ? and return_time = 0";  
	String removeBook = "delete from book where book_id = ?";  
Connection connection = connector.getConn();  
  
//check  
	try {  
		PreparedStatement bookStatement = connection.prepareStatement(checkBook);  
		bookStatement.setInt(1, bookId);  
		ResultSet resultSet = bookStatement.executeQuery();  
		if(resultSet.next()) {  
			rollback(connection);  
			return new ApiResult(false, "Not returned yet!");  
		}  
	} catch (SQLException e) {  
		throw new RuntimeException(e);  
	}  
  
	//remove  
	try {  
		PreparedStatement bookStatement = connection.prepareStatement(removeBook);  
		bookStatement.setInt(1, bookId);  
	// commit(connection);  
		int deleteRows = bookStatement.executeUpdate();  
	if(deleteRows == 0) {  
		rollback(connection);  
		return new ApiResult(false, "remove failed!");  
	}  
		commit(connection);  
		return new ApiResult(true, "remove successfully!");  
} catch (SQLException e) {  
// throw new RuntimeException(e);  
rollback(connection);  
return new ApiResult(false, "remove failed");  
}  
// return new ApiResult(false, "Unimplemented Function");  
}
```

### 5. 更改图书信息
#### 要求
+ 对书库中的对应书籍进行信息修改
#### 原理
+ 
#### source code
```Java
public ApiResult modifyBookInfo(Book book) {  
String updateBook = "update book set category = ? , title = ? , press = ? , publish_year = ? , author = ? , price = ? WHERE book_id = ?";  
  
Connection connection = connector.getConn();  
try{  
PreparedStatement bookStatement = connection.prepareStatement(updateBook);  
bookStatement.setString(1, book.getCategory());  
bookStatement.setString(2, book.getTitle());  
bookStatement.setString(3, book.getPress());  
bookStatement.setInt(4, book.getPublishYear());  
bookStatement.setString(5, book.getAuthor());  
bookStatement.setDouble(6, book.getPrice());  
// bookStatement.setInt(7, book.getStock());  
bookStatement.setInt(7, book.getBookId());  
// ResultSet resultSet = bookStatement.executeQuery();  
int newInfo = bookStatement.executeUpdate();  
if(newInfo == 0){  
throw new SQLException("Modify failed, no such book!");  
}  
commit(connection);  
return new ApiResult(true, "modified successfully!");  
} catch (SQLException e) {  
rollback(connection);  
e.printStackTrace();  
return new ApiResult(false, "Error modifying book: " + e.getMessage());  
}  
  
// return new ApiResult(false, "Unimplemented Function");  
}
```


### GUI 
#### 
# 实验成果
### GUI
+ 图书相关操作
![](pic/book.png)

+ 借书相关操作
![](pic/borrow.png)

+ 借书卡相关操作
![](card.png)