import entities.Book;
import entities.Borrow;
import entities.Card;
import queries.*;
import utils.DBInitializer;
import utils.DatabaseConnector;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class LibraryManagementSystemImpl implements LibraryManagementSystem {

    private final DatabaseConnector connector;

    public LibraryManagementSystemImpl(DatabaseConnector connector) {
        this.connector = connector;
    }

    @Override
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

            try (ResultSet generatedKeys = bookStatement.getGeneratedKeys()) {//获取主键，赋值回book
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
//            throw new RuntimeException(e);
        }
    }

    @Override
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
//                    deltaStock = -currentStock;
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

    @Override
    public ApiResult storeBook(List<Book> books) {
        String checkBook = "SELECT * FROM book WHERE category = ? AND title = ? AND press = ? AND publish_year = ? AND author = ?";
        String insertBook = "INSERT INTO book (category, title, press, publish_year, author, price, stock) VALUES (?, ?, ?, ?, ?, ?, ?)";
        Connection connection = connector.getConn();

        try {
            connection.setAutoCommit(false);
            PreparedStatement bookStatement = connection.prepareStatement(checkBook);
            PreparedStatement insertBookStatement = connection.prepareStatement(insertBook, Statement.RETURN_GENERATED_KEYS);

            for (Book book : books) {
                bookStatement.setString(1, book.getCategory());
                bookStatement.setString(2, book.getTitle());
                bookStatement.setString(3, book.getPress());
                bookStatement.setInt(4, book.getPublishYear());
                bookStatement.setString(5, book.getAuthor());
                ResultSet resultSet = bookStatement.executeQuery();

                if (resultSet.next()) {
                    rollback(connection);
                    return new ApiResult(false, "Error! Already Exists!");
                }

                insertBookStatement.setString(1, book.getCategory());
                insertBookStatement.setString(2, book.getTitle());
                insertBookStatement.setString(3, book.getPress());
                insertBookStatement.setInt(4, book.getPublishYear());
                insertBookStatement.setString(5, book.getAuthor());
                insertBookStatement.setDouble(6, book.getPrice());
                insertBookStatement.setDouble(7, book.getStock());

                int newRow = insertBookStatement.executeUpdate();
                if (newRow == 0) {
                    throw new SQLException("Creating book failed, no rows affected.");
                }

                try (ResultSet generatedKeys = insertBookStatement.getGeneratedKeys()) {
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


    @Override
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
                return new ApiResult(false, "This book has not been returned yet!");
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        //remove
        try {
            PreparedStatement bookStatement = connection.prepareStatement(removeBook);
            bookStatement.setInt(1, bookId);
//            commit(connection);
            int deleteRows = bookStatement.executeUpdate();
            if(deleteRows == 0) {
                rollback(connection);
                return new ApiResult(false, "Removed Failed! No such book!");
            }
            commit(connection);
            return new ApiResult(true, "Remove Successfully!");
        } catch (SQLException e) {
            rollback(connection);
            return new ApiResult(false, "Remove Failed!");
        }
    }

    @Override
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
//            bookStatement.setInt(7, book.getStock());
            bookStatement.setInt(7, book.getBookId());
//            ResultSet resultSet = bookStatement.executeQuery();
            int newInfo = bookStatement.executeUpdate();
            if(newInfo == 0){
                return new ApiResult(false, "Modify failed, no such book!");
            }
            commit(connection);
            return new ApiResult(true, "Modified successfully!");
        } catch (SQLException e) {
            rollback(connection);
            e.printStackTrace();
            return new ApiResult(false, "Error modifying book: " + e.getMessage());
        }

        //        return new ApiResult(false, "Unimplemented Function");
    }

    @Override//注意模糊查询！！！
    public ApiResult queryBook(BookQueryConditions conditions) {
        StringBuilder queryBuilder = new StringBuilder("select * from book where 1=1"); // 1=1 占位
        Connection connection = connector.getConn();
        List<Object> queryParams = new ArrayList<>();

        if(conditions.getCategory() != null){
            queryBuilder.append(" AND category = ?"); //追加条件
            queryParams.add(conditions.getCategory());
        }
        if(conditions.getTitle() != null){
            queryBuilder.append(" AND title LIKE ?"); //模糊查询
            queryParams.add("%" + conditions.getTitle() + "%");
        }
        if(conditions.getPress() != null){
            queryBuilder.append(" AND press LIKE ?"); //模糊查询
            queryParams.add("%" + conditions.getPress() + "%");
        }
        if(conditions.getMaxPublishYear() != null){
            queryBuilder.append(" AND publish_year <= ?"); //追加条件
            queryParams.add(conditions.getMaxPublishYear());
        }
        if(conditions.getMinPublishYear() != null){
            queryBuilder.append(" AND publish_year >= ?"); //追加条件
            queryParams.add(conditions.getMinPublishYear());
        }
        if(conditions.getAuthor() != null){
            queryBuilder.append(" AND author LIKE ?"); //模糊查询
            queryParams.add("%" + conditions.getAuthor() + "%");
        }
        if(conditions.getMaxPrice() != null){
            queryBuilder.append(" AND price <= ?"); //追加条件
            queryParams.add(conditions.getMaxPrice());
        }
        if(conditions.getMinPrice() != null) {
            queryBuilder.append(" AND price >= ?"); //追加条件
            queryParams.add(conditions.getMinPrice());
        }
        if(conditions.getSortBy() != null && conditions.getSortOrder() != null) {
            queryBuilder.append(" order by " + conditions.getSortBy().toString().toLowerCase() + " " + conditions.getSortOrder().toString());
        }
        if(!conditions.getSortBy().toString().toLowerCase().equals("book_id")) { //检查是否已经是bookid,如果已经是就不加二级排序
            queryBuilder.append(", book_id ASC");
        }
        String queryBook = queryBuilder.toString();
        try {
            PreparedStatement bookStatement = connection.prepareStatement(queryBook);
            for(int i = 1; i <= queryParams.size(); i++) {
                bookStatement.setObject(i, queryParams.get(i - 1));
            }
            ResultSet resultSet = bookStatement.executeQuery();
            List<Book> books = new ArrayList<>();
            while (resultSet.next()) {
                Book tmp = new Book();
                tmp.setBookId(resultSet.getInt("book_id"));
                tmp.setCategory(resultSet.getString("category"));
                tmp.setPress(resultSet.getString("press"));
                tmp.setAuthor(resultSet.getString("author"));
                tmp.setPublishYear(resultSet.getInt("publish_year"));
                tmp.setTitle(resultSet.getString("title"));
                tmp.setPrice(resultSet.getDouble("price"));
                tmp.setStock(resultSet.getInt("stock"));
                books.add(tmp);
//                System.out.println("Book:" + tmp.toString());
            }

            BookQueryResults queryResults = new BookQueryResults(books);
            queryResults.setCount(books.size());
//            System.out.println(books.size());
            commit(connection);

            return new ApiResult(true, "query now!", queryResults);
        } catch (SQLException e) {
            rollback(connection);
            e.printStackTrace();
            return new ApiResult(false, "Query failed!" + e.getMessage(), null);
        }
    }

    @Override
    public ApiResult borrowBook(Borrow borrow) {
        String checkID = "select * from book where book_id = ?";
        String checkCard = "select * from card where card_id = ?";
        String checkBorrowBook = "select * from borrow where card_id = ? and book_id = ? and return_time = 0";
        String insertBorrowBook = "insert into borrow (card_id, book_id, borrow_time, return_time) values (?, ?, ?, ?)";
        String checkStockForUpdate = "SELECT * FROM book WITH (UPDLOCK, ROWLOCK) WHERE book_id = ? AND stock > 0";

        String updateStock = "update book set stock = stock - 1 where book_id = ?";
        Connection connection = connector.getConn();
        //check id
        try {
            PreparedStatement bookIDStatement = connection.prepareStatement(checkID);
            bookIDStatement.setInt(1, borrow.getBookId());
            ResultSet bookResultSet = bookIDStatement.executeQuery();
            if(bookResultSet.next()) {
                //找到存在的bookID，可以继续借书
            } else {
                rollback(connection);
                return new ApiResult(false, "No such book!");
            }
            PreparedStatement cardStatement = connection.prepareStatement(checkCard);
            cardStatement.setInt(1, borrow.getCardId());
            ResultSet resultSet = cardStatement.executeQuery();
            if(resultSet.next()) {

            }
            else{
                rollback(connection);
                return new ApiResult(false, "No such card!");
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        //check borrow
        try {
            PreparedStatement bookStatement = connection.prepareStatement(checkBorrowBook);
            bookStatement.setInt(1, borrow.getCardId());
            bookStatement.setInt(2, borrow.getBookId());
            ResultSet resultSet = bookStatement.executeQuery();
            if(resultSet.next()) {
                rollback(connection);
                return new ApiResult(false, "You have not returned the book yet!");
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        //check and update stock
        try {
            connection.setAutoCommit(false);

            PreparedStatement bookStatement = connection.prepareStatement(checkStockForUpdate);
            bookStatement.setInt(1, borrow.getBookId());
            ResultSet resultSet = bookStatement.executeQuery();

            if (!resultSet.next()) {
                rollback(connection);
                return new ApiResult(false, "No such book!");
            }

            int stock = resultSet.getInt("stock");
            if (stock <= 0) {
                rollback(connection);
                connection.setAutoCommit(true);
                return new ApiResult(false, "No more book to borrow!");
            }

            // Update stock
            PreparedStatement updateStockStatement = connection.prepareStatement(updateStock);
            updateStockStatement.setInt(1, borrow.getBookId());
            updateStockStatement.executeUpdate();

        } catch (SQLException e) {
            rollback(connection);
//            connection.setAutoCommit(true);
            throw new RuntimeException(e);
        }

        //insert
        try {
            connection.setAutoCommit(false);
            PreparedStatement bookStatement = connection.prepareStatement(insertBorrowBook, Statement.RETURN_GENERATED_KEYS);
            PreparedStatement stockStatement = connection.prepareStatement(updateStock);
            bookStatement.setInt(1, borrow.getCardId());
            bookStatement.setInt(2, borrow.getBookId());
            bookStatement.setLong(3, borrow.getBorrowTime());
            bookStatement.setLong(4, 0);
            int newRow = bookStatement.executeUpdate();
            if(newRow > 0) {
                commit(connection);
                return new ApiResult(true, "Borrowed successfully!");
            } else {
                rollback(connection);
                return new ApiResult(false, "Borrow failed!");
            }
        } catch (SQLException e) {
            rollback(connection);
            e.printStackTrace();
            return new ApiResult(false, "Borrow failed!");
        }
    }

    @Override
    public ApiResult returnBook(Borrow borrow) {
        String checkBorrowBook = "select * from borrow where card_id = ? and book_id = ? and return_time = 0";
        String updateBorrowBook = "update borrow set return_time = ? where card_id = ? and book_id = ? and return_time = 0";
        String updateStock = "update book set stock = stock + 1 where book_id = ?";
        Connection connection = connector.getConn();
        //check id and get borrow time
        Long tmp = borrow.getBorrowTime();
        try {
            PreparedStatement checkStatement = connection.prepareStatement(checkBorrowBook);
            checkStatement.setInt(1, borrow.getCardId());
            checkStatement.setInt(2, borrow.getBookId());
            ResultSet resultSet = checkStatement.executeQuery();
            if (!resultSet.next()) {
                rollback(connection);
                return new ApiResult(false, "No book to be returned!");
            } else {
                tmp = resultSet.getLong("borrow_time");
                if (tmp >= borrow.getReturnTime()) {
                    rollback(connection);
                    return new ApiResult(false, "Return time error!");
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        try {
            PreparedStatement borrowStatement = connection.prepareStatement(updateBorrowBook);
            PreparedStatement stockStatement = connection.prepareStatement(updateStock);
            stockStatement.setInt(1, borrow.getBookId());
            borrowStatement.setLong(1, borrow.getReturnTime());
            borrowStatement.setInt(2, borrow.getCardId());
            borrowStatement.setInt(3, borrow.getBookId());
            int newRow = borrowStatement.executeUpdate();
            if (newRow > 0) {
                stockStatement.executeUpdate();
                commit(connection);
                return new ApiResult(true, "Return successfully!");
            } else {
                return new ApiResult(false, "Return failed!");
            }
        } catch (SQLException e) {
            rollback(connection);
            e.printStackTrace();
            return new ApiResult(false, "Return failed!");
        }
    }

    @Override
    public ApiResult showBorrowHistory(int cardId) {
        String borrowHistory = "select * from borrow where card_id = ? order by borrow_time DESC, book_id ASC";
        String bookSelected = "select * from book where book_id = ?";
        Connection connection = connector.getConn();

        try{
            PreparedStatement borrowStatement = connection.prepareStatement(borrowHistory);
            PreparedStatement bookStatement = connection.prepareStatement(bookSelected);
            borrowStatement.setInt(1, cardId);
            ResultSet resultSet = borrowStatement.executeQuery();
            List<BorrowHistories.Item> borrows = new ArrayList<>();
            while (resultSet.next()) {
                bookStatement.setInt(1, resultSet.getInt("book_id"));
                ResultSet resultSet1 = bookStatement.executeQuery();
                BorrowHistories.Item tmp = new BorrowHistories.Item();
                tmp.setBookId(resultSet.getInt("book_id"));
                tmp.setCardId(resultSet.getInt("card_id"));
                if(resultSet1.next()){
                    tmp.setCategory(resultSet1.getString("category"));
                    tmp.setTitle(resultSet1.getString("title"));
                    tmp.setPress(resultSet1.getString("press"));
                    tmp.setPublishYear(resultSet1.getInt("publish_year"));
                    tmp.setAuthor(resultSet1.getString("author"));
                    tmp.setPrice(resultSet1.getDouble("price"));
                }
                tmp.setBorrowTime(resultSet.getLong("borrow_time"));
                tmp.setReturnTime(resultSet.getLong("return_time"));
                borrows.add(tmp);
            }
            BorrowHistories borrowHistories = new BorrowHistories(borrows);
            borrowHistories.setCount(borrows.size());
            commit(connection);
            return new ApiResult(true, "showed successfully!", borrowHistories);
        } catch (SQLException e) {
            rollback(connection);
            e.printStackTrace();
            return new ApiResult(false, "Showing failed!");
        }
    }

    @Override
    public ApiResult registerCard(Card card) {
        String checkCard = "select * from card where name = ? and department = ? and type = ?";
        String insertCard = "insert into card (name, department, type) values (?, ?, ?)";
        Connection connection = connector.getConn();

        //check
        try {
            PreparedStatement bookStatement = connection.prepareStatement(checkCard);
            bookStatement.setString(1, card.getName());
            bookStatement.setString(2, card.getDepartment());
            bookStatement.setString(3, card.getType().getStr());
            ResultSet resultSet = bookStatement.executeQuery();
            if(resultSet.next()) { //找到了一样的
                rollback(connection);
                return new ApiResult(false, "Error! Card already Exists!");
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        try {
            PreparedStatement bookStatement = connection.prepareStatement(insertCard, Statement.RETURN_GENERATED_KEYS);// 补全占位符，获取自动主键
            bookStatement.setString(1, card.getName());
            bookStatement.setString(2, card.getDepartment());
            bookStatement.setString(3, card.getType().getStr());
            int newRow = bookStatement.executeUpdate();
            if (newRow == 0) {
                throw new SQLException("Creating book failed, no rows affected.");
            }

            try (ResultSet generatedKeys = bookStatement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    card.setCardId(generatedKeys.getInt(1));
                } else {
                    throw new SQLException("Creating card failed, no ID obtained.");
                }
            }
            commit(connection);
            return new ApiResult(true, "Registered successfully");
        } catch (SQLException e) {
            rollback(connection);
            e.printStackTrace();
            return new ApiResult(false, "Error registering card: " + e.getMessage());
//            throw new RuntimeException(e);
        }
    }

    @Override
    public ApiResult removeCard(int cardId) {
        String removeCard = "delete from card where card_id = ?";
        String checkCard = "select * from borrow where card_id = ? and return_time = 0";
        Connection connection = connector.getConn();
        try {
            PreparedStatement cardStatement = connection.prepareStatement(checkCard);
            cardStatement.setInt(1, cardId);
            ResultSet resultSet = cardStatement.executeQuery();
            if(resultSet.next()) {
                rollback(connection);
                return new ApiResult(false, "Remove card failed!");
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        try {
            PreparedStatement cardStatement = connection.prepareStatement(removeCard);
            cardStatement.setInt(1, cardId);
            int newRow = cardStatement.executeUpdate();
            if(newRow > 0) {
                commit(connection);
                return new ApiResult(true, "removeCard successfully!");
            } else {
                rollback(connection);
                return new ApiResult(false, "Remove card failed!");
            }
        } catch (SQLException e) {
            rollback(connection);
            e.printStackTrace();
//            throw new RuntimeException(e);
            return new ApiResult(false, "Remove card failed!");
        }
//        return new ApiResult(false, "Unimplemented Function");
    }

    @Override
    public ApiResult showCards() {
        String cardQuery = "select * from card order by card_id ASC";
        Connection connection = connector.getConn();

        try {
            PreparedStatement cardStatement = connection.prepareStatement(cardQuery);
            ResultSet resultSet = cardStatement.executeQuery();
            List<Card> cards = new ArrayList<>();
            while (resultSet.next()) {
                Card tmp = new Card();
                tmp.setCardId(resultSet.getInt("card_id"));
                tmp.setName(resultSet.getString("name"));
                tmp.setDepartment(resultSet.getString("department"));
                tmp.setType(Card.CardType.values(resultSet.getString("type")));
                cards.add(tmp);
            }
            CardList cardList = new CardList(cards);
            cardList.setCount(cards.size());
            commit(connection);
            return new ApiResult(true, "showed!", cardList);
        } catch (SQLException e) {
            rollback(connection);
            e.printStackTrace();
            return new ApiResult(false, "Show cards failed!");
        }
    }

    @Override
    public ApiResult resetDatabase() {
        Connection conn = connector.getConn();
        try {
            Statement stmt = conn.createStatement();
            DBInitializer initializer = connector.getConf().getType().getDbInitializer();
            stmt.addBatch(initializer.sqlDropBorrow());
            stmt.addBatch(initializer.sqlDropBook());
            stmt.addBatch(initializer.sqlDropCard());
            stmt.addBatch(initializer.sqlCreateCard());
            stmt.addBatch(initializer.sqlCreateBook());
            stmt.addBatch(initializer.sqlCreateBorrow());
            stmt.executeBatch();
            commit(conn);
        } catch (Exception e) {
            rollback(conn);
            return new ApiResult(false, e.getMessage());
        }
        return new ApiResult(true, null);
    }

    private void rollback(Connection conn) {
        try {
            conn.rollback();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void commit(Connection conn) {
        try {
            conn.commit();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}
