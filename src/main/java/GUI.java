import entities.Book;
import entities.Borrow;
import entities.Card;
import org.apache.commons.lang3.RandomUtils;
import queries.*;
import queries.SortOrder;
import utils.ConnectConfig;
import utils.DatabaseConnector;
import utils.RandomData;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.ResultSet;
import java.util.*;
import java.util.List;

public class GUI {

    private DatabaseConnector connector;
    private LibraryManagementSystem library;
    private static ConnectConfig connectConfig = null;
    private JTable bookTable;
    private JTable borrowTable;
    private JTable cardTable;

    static {
        try {
            // parse connection config from "resources/application.yaml"
            connectConfig = new ConnectConfig();
            System.out.println("Connected!");
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

    public GUI() {
        try {
            // connect to database
            connector = new DatabaseConnector(connectConfig);
            library = new LibraryManagementSystemImpl(connector);
            System.out.println("Successfully init class BookTest.");
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }

        boolean connStatus = connector.connect();
        System.out.println("Successfully connect to database.");
        ApiResult result = library.resetDatabase();
        if (!result.ok) {
            System.out.printf("Failed to reset database, reason: %s\n", result.message);
        }
        System.out.println("Successfully reset database.");

        initBookTable();
        refreshBook();
        initBorrowTable();
        refreshBorrow();
        initCardTable();
        refreshCard();
        JFrame frame = new JFrame("Library Management System");
        frame.setSize(1200, 900);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setResizable(false); //disable resizing

        JPanel cards = new JPanel(new CardLayout());
        frame.add(cards);

        JPanel mainPanel = new JPanel();
        storeBookPanel storeBook = new storeBookPanel(cards);
        borrowBookPanel borrowBook = new borrowBookPanel(cards);
        registerCard registerCards = new registerCard(cards);

        placeComponents(mainPanel, storeBook, borrowBook, registerCards, cards);

        cards.add(mainPanel, "main");
        cards.add(storeBook, "storeBook");
        cards.add(borrowBook, "borrowBook");
        cards.add(registerCards, "registerBook");

        frame.setVisible(true);
    }
    public void initBookTable() {
        String[] columnNames = {"Book ID", "Category", "Title", "Press", "Publish Year", "Author", "Price", "Stock"};
        DefaultTableModel tableModel = new DefaultTableModel(columnNames, 0);
        bookTable = new JTable(tableModel);
    }

    public void initBorrowTable() {
        String[] columnNames = {"Card ID", "Book ID", "Category", "Title", "Press", "Publish Year", "Author", "Price", "Borrow Time", "Return Time"};
        DefaultTableModel tableModel = new DefaultTableModel(columnNames, 0);
        borrowTable = new JTable(tableModel);
    }

    private void initCardTable() {
        String[] columnNames = {"Card ID", "Name", "Department", "Type"};
        DefaultTableModel tableModel = new DefaultTableModel(columnNames, 0);
        cardTable = new JTable(tableModel);
    }

    public void addBookTable(JPanel panel) {
        JScrollPane scrollPane = new JScrollPane(bookTable);
        scrollPane.setBounds(500, 25, 670, 800);
        panel.add(scrollPane);
    }

    public void addBorrowTable(JPanel panel) {
        JScrollPane scrollPane = new JScrollPane(borrowTable);
        scrollPane.setBounds(400, 25, 770, 800);
        panel.add(scrollPane);
    }

    public void addCardTable(JPanel panel) {
        JScrollPane scrollPane = new JScrollPane(cardTable);
        scrollPane.setBounds(400, 25, 670, 800);
        panel.add(scrollPane);
    }
    public void refreshBook() {
        DefaultTableModel tableModel = (DefaultTableModel) bookTable.getModel();
        tableModel.setRowCount(0);
        BookQueryConditions conditions = new BookQueryConditions();
        ApiResult result = library.queryBook(conditions);
        if(result.ok) {
            BookQueryResults queryResults = (BookQueryResults) result.payload;
            List<Book> books = queryResults.getResults();
            for(Book book:books) {
                tableModel.addRow(new Object[]{
                        book.getBookId(),
                        book.getCategory(),
                        book.getTitle(),
                        book.getPress(),
                        book.getPublishYear(),
                        book.getAuthor(),
                        book.getPrice(),
                        book.getStock()
                });
            }
        }
    }
    public void refreshBook(BookQueryConditions conditions) {
        DefaultTableModel tableModel = (DefaultTableModel) bookTable.getModel();
        tableModel.setRowCount(0);
        ApiResult result = library.queryBook(conditions);
        if(result.ok) {
            BookQueryResults queryResults = (BookQueryResults) result.payload;
            List<Book> books = queryResults.getResults();
            for(Book book:books) {
                tableModel.addRow(new Object[]{
                        book.getBookId(),
                        book.getCategory(),
                        book.getTitle(),
                        book.getPress(),
                        book.getPublishYear(),
                        book.getAuthor(),
                        book.getPrice(),
                        book.getStock()
                });
            }
        }
    }

    public void refreshBorrow(){//show all borrow History
        DefaultTableModel tableModel = (DefaultTableModel) borrowTable.getModel();
        tableModel.setRowCount(0);
        ApiResult result = library.showCards(); // get all cards id first
        if(result.ok) {
            CardList cardList = (CardList) result.payload;
            List<Card> cards = cardList.getCards();
            for(Card card:cards) {
                ApiResult borrowResult = library.showBorrowHistory(card.getCardId());
                if(borrowResult.ok) {
                    BorrowHistories borrowHistories = (BorrowHistories) borrowResult.payload;
                    List<BorrowHistories.Item> borrows = borrowHistories.getItems();
                    for(BorrowHistories.Item item:borrows){
                        tableModel.addRow(new Object[]{
                                item.getCardId(),
                                item.getBookId(),
                                item.getCategory(),
                                item.getTitle(),
                                item.getPress(),
                                item.getPublishYear(),
                                item.getAuthor(),
                                item.getPrice(),
                                item.getBorrowTime(),
                                item.getReturnTime()
                        });
                    }
                }
            }
        }
    }
    public void refreshBorrow(int cardID){ //override for showHistory by cardID
        DefaultTableModel tableModel = (DefaultTableModel) borrowTable.getModel();
        tableModel.setRowCount(0);
        ApiResult borrowResult = library.showBorrowHistory(cardID);
        if(borrowResult.ok) {
            BorrowHistories borrowHistories = (BorrowHistories) borrowResult.payload;
            List<BorrowHistories.Item> borrows = borrowHistories.getItems();
            for(BorrowHistories.Item item:borrows){
                tableModel.addRow(new Object[]{
                        item.getCardId(),
                        item.getBookId(),
                        item.getCategory(),
                        item.getTitle(),
                        item.getPress(),
                        item.getPublishYear(),
                        item.getAuthor(),
                        item.getPrice(),
                        item.getBorrowTime(),
                        item.getReturnTime()
                });
            }
        }
    }

    public void refreshCard() {
        DefaultTableModel tableModel = (DefaultTableModel) cardTable.getModel();
        tableModel.setRowCount(0);
        ApiResult result = library.showCards();
        if(result.ok) {
            CardList cardList = (CardList) result.payload;
            List<Card> cards = cardList.getCards();
            for(Card card:cards) {
                tableModel.addRow(new Object[] {
                        card.getCardId(),
                        card.getName(),
                        card.getDepartment(),
                        card.getType().getStr()
                });
            }
        }
    }
    public class printErrorMessage {
        public void showMessage(Component parentComponent, String errorMessage, String title) {
            JOptionPane.showMessageDialog(parentComponent, errorMessage, title, JOptionPane.ERROR_MESSAGE);
        }
    }

    public class registerCard extends JPanel {
        public registerCard(JPanel cards) {
            setLayout(null);
            ActionListener returnToMainAction = new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    CardLayout cardLayout = (CardLayout) cards.getLayout();
                    cardLayout.show(cards, "main");
                }
            };
            ActionListener refreshAction = new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    refreshBorrow();
                }
            };

            JButton refreshButton = refreshButton("REFRESH", refreshAction);
            add(refreshButton);

            JLabel name = new JLabel("Name");
            name.setBounds(100, 100, 70, 30);
            add(name);
            JTextField cardName = new JTextField(70);
            cardName.setBounds(170,100,100,30);
            add(cardName);

            JLabel department = new JLabel("Department");
            department.setBounds(100, 130, 70, 30);
            add(department);
            JTextField cardDepartment = new JTextField(70);
            cardDepartment.setBounds(170,130,100,30);
            add(cardDepartment);

            JLabel type = new JLabel("Type");
            type.setBounds(100, 160, 70, 30);
            add(type);
            JTextField cardType = new JTextField(70);
            cardType.setBounds(170,160,100,30);
            add(cardType);

            JButton registerButton = new JButton("Register");
            registerButton.setBounds(100, 190, 170, 30);
            add(registerButton);

            JLabel cardID = new JLabel("Card ID");
            cardID.setBounds(100, 220, 70, 30);
            add(cardID);
            JTextField IDText = new JTextField(50);
            IDText.setBounds(170, 220, 100, 30);
            add(IDText);

            JButton removeButton = new JButton("Remove ID");
            removeButton.setBounds(100, 250, 170, 30);
            add(removeButton);

            JButton returnMain = addReturnButton("RETURN", returnToMainAction);
            returnMain.setBounds(100, 650, 170, 30);
            add(returnMain);
            addCardTable(this);

            registerButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    String name = cardName.getText();
                    String department = cardDepartment.getText();
                    Card.CardType type = Card.CardType.values(cardType.getText());

                    Card c0 = new Card(-1, name, department, type);
                    library.registerCard(c0);
                    //clear
                    cardName.setText("");
                    cardDepartment.setText("");
                    cardType.setText("");
                    refreshCard();
                }
            });

            removeButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    int cardID = Integer.parseInt(IDText.getText());
                    library.removeCard(cardID);
                    //clear
                    IDText.setText("");
                    refreshCard();
                }
            });
            refreshCard();
        }
    }
    public class storeBookPanel extends JPanel {
        public storeBookPanel(JPanel cards) {
            setLayout(null);
            ActionListener returnToMainAction = new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    CardLayout cardLayout = (CardLayout) cards.getLayout();
                    cardLayout.show(cards, "main");
                }
            };

            ActionListener refreshAction = new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    refreshBorrow();
                }
            };

            JButton refreshButton = refreshButton("REFRESH", refreshAction);
            add(refreshButton);

            JLabel Category = new JLabel("Category");
            Category.setBounds(100, 100, 70, 30);
            add(Category);
            JTextField bookCategory = new JTextField(70);
            bookCategory.setBounds(170,100,100,30);

            JLabel Title = new JLabel("Title");
            Title.setBounds(100, 130, 70, 30);
            add(Title);
            JTextField bookTitle = new JTextField(70);
            bookTitle.setBounds(170,130,100,30);

            JLabel Press = new JLabel("Press");
            Press.setBounds(100, 160, 70, 30);
            add(Press);
            JTextField bookPress = new JTextField(70);
            bookPress.setBounds(170,160,100,30);

            JLabel PublishYear = new JLabel("PublishYear");
            PublishYear.setBounds(100, 190, 70, 30);
            add(PublishYear);
            JTextField bookPublishYear = new JTextField(70);
            bookPublishYear.setBounds(170,190,100,30);

            JLabel Author = new JLabel("Author");
            Author.setBounds(100, 220, 70, 30);
            add(Author);
            JTextField bookAuthor = new JTextField(70);
            bookAuthor.setBounds(170,220,100,30);

            JLabel Price = new JLabel("Price");
            Price.setBounds(100, 250, 70, 30);
            add(Price);
            JTextField bookPrice = new JTextField(70);
            bookPrice.setBounds(170, 250, 100,30);

            JLabel Stock = new JLabel("Stock");
            Stock.setBounds(100, 280, 70, 30);
            add(Stock);
            JTextField bookStock = new JTextField(70);
            bookStock.setBounds(170,280,100,30);

            add(bookCategory);
            add(bookTitle);
            add(bookPress);
            add(bookPublishYear);
            add(bookAuthor);
            add(bookPrice);
            add(bookStock);
            addBookTable(this);

            JButton storeBookButton = new JButton("Store Book");
            storeBookButton.setBounds(100, 310, 170, 30);
            add(storeBookButton);

            JLabel bookID = new JLabel("bookID");
            bookID.setBounds(100, 340, 70, 30);
            add(bookID);
            JTextField BookID = new JTextField(70);
            BookID.setBounds(170, 340, 100, 30);
            add(BookID);

            JButton removeBookButton = new JButton("Remove Book");
            removeBookButton.setBounds(100, 370, 170, 30);
            add(removeBookButton);

            JButton modifyButton = new JButton("Modify Book");
            modifyButton.setBounds(100, 400, 170, 30);
            add(modifyButton);

            JButton queryButton = new JButton("Query Book");
            queryButton.setBounds(100, 550, 170, 30);
            add(queryButton);

            JLabel rangePublishYear = new JLabel("PublishYear");
            rangePublishYear.setBounds(100, 430, 70, 30);
            add(rangePublishYear);

            JTextField bookMinPublishYear = new JTextField(70);
            bookMinPublishYear.setBounds(170, 430, 45, 30);
            add(bookMinPublishYear);

            JLabel pubL = new JLabel("--");
            pubL.setBounds(215, 430, 10, 30);
            add(pubL);

            JTextField bookMaxPublishYear = new JTextField(70);
            bookMaxPublishYear.setBounds(225, 430, 45, 30);
            add(bookMaxPublishYear);

            JLabel rangePrice = new JLabel("Price");
            rangePrice.setBounds(100, 460, 70, 30);
            add(rangePrice);

            JTextField bookMinPrice = new JTextField(70);
            bookMinPrice.setBounds(170, 460, 45, 30);
            add(bookMinPrice);

            JLabel priceL = new JLabel("--");
            priceL.setBounds(215, 460, 10, 30);
            add(priceL);

            JTextField bookMaxPrice = new JTextField(70);
            bookMaxPrice.setBounds(225, 460, 45, 30);
            add(bookMaxPrice);

            JButton bulkStoreBookButton = new JButton("Bulk Store Book");
            bulkStoreBookButton.setBounds(100, 600, 170, 30);
            add(bulkStoreBookButton);

            JButton returnMain = addReturnButton("RETURN", returnToMainAction);
            returnMain.setBounds(100, 650, 170, 30);
            add(returnMain);

            JLabel sortCondition = new JLabel("Sort By");
            sortCondition.setBounds(100, 490, 70, 30);
            add(sortCondition);

            String[] options = {"Book ID", "Category", "Title", "Press", "PublishYear", "Author", "Price"};
            JComboBox<String> sortOptions = new JComboBox<>(options);
            sortOptions.setBounds(170, 490, 100, 30);
            add(sortOptions);

            JLabel sortOrder = new JLabel("Sort Order");
            sortOrder.setBounds(100, 520, 70, 30);
            add(sortOrder);

            String[] orderOptions = {"升序", "降序"};
            JComboBox<String> sortOrderBox = new JComboBox<>(orderOptions);
            sortOrderBox.setBounds(170, 520, 100, 30);
            add(sortOrderBox);
            storeBookButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    String category = bookCategory.getText();
                    String title = bookTitle.getText();
                    String press = bookPress.getText();
                    int publishYear = Integer.parseInt(bookPublishYear.getText());
                    String author = bookAuthor.getText();
                    double price = Double.parseDouble(bookPrice.getText());
                    int stock = Integer.parseInt(bookStock.getText());
                    Book b0 = new Book(category, title, press, publishYear, author, price, stock);
                    ApiResult result = library.storeBook(b0);
                    if(!result.ok) {
                        JOptionPane.showMessageDialog(storeBookPanel.this, result.message, "Store Book Failure", JOptionPane.ERROR_MESSAGE);
                    }
                    //clear
                    bookCategory.setText("");
                    bookTitle.setText("");
                    bookPress.setText("");
                    bookPublishYear.setText("");
                    bookAuthor.setText("");
                    bookPrice.setText("");
                    bookStock.setText("");
                    refreshBook();
                }
            });
            bulkStoreBookButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    int nOps = 1000;
                    Set<Book> bookSet = new HashSet<>();
                    for (int i = 0; i < nOps; i++) {
                        bookSet.add(RandomData.randomBook());
                    }
                    /* provide some duplicate records */
                    List<Book> bookList1 = new ArrayList<>(bookSet);
                    for (int i = 0; i < 3; i++) {
                        Book cb = bookList1.get(new Random().nextInt(bookList1.size())).clone();
                        // randomly change some attributes
                        if (RandomUtils.nextBoolean()) {
                            cb.setStock(RandomUtils.nextInt(0, 20));
                            cb.setPrice(RandomUtils.nextDouble(6.66, 233.33));
                        }
                        bookList1.add(cb);
                    }
                    Collections.shuffle(bookList1);
                    ApiResult result1 = library.storeBook(bookList1);
                    if(!result1.ok) {
                        JOptionPane.showMessageDialog(storeBookPanel.this, result1.message, "Store Book Failure", JOptionPane.ERROR_MESSAGE);
                    }
                    /* make sure that none of the books are inserted */
                    ApiResult queryResult1 = library.queryBook(new BookQueryConditions());
                    BookQueryResults selectedResults1 = (BookQueryResults) queryResult1.payload;
                    /* normal batch insert */
                    List<Book> bookList2 = new ArrayList<>(bookSet);
                    ApiResult result2 = library.storeBook(bookList2);
                    if(!result2.ok) {
                        JOptionPane.showMessageDialog(storeBookPanel.this, result2.message, "Store Book Failure", JOptionPane.ERROR_MESSAGE);
                    }
                    ApiResult queryResult2 = library.queryBook(new BookQueryConditions());
                    bookList2.sort(Comparator.comparingInt(Book::getBookId));
                    refreshBook();
                }
            });
            removeBookButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    int bookID = Integer.parseInt(BookID.getText());
                    library.removeBook(bookID);
                    //clear
                    BookID.setText("");
                    refreshBook();
                }
            });
            modifyButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    String category = bookCategory.getText();
                    String title = bookTitle.getText();
                    String press = bookPress.getText();
                    int publishYear = Integer.parseInt(bookPublishYear.getText());
                    String author = bookAuthor.getText();
                    double price = Double.parseDouble(bookPrice.getText());
                    int stock = Integer.parseInt(bookStock.getText());
                    int bookID = Integer.parseInt(BookID.getText());
                    Book b0 = new Book(category, title, press, publishYear, author, price, stock);
                    b0.setBookId(bookID);
                    library.modifyBookInfo(b0);
                    //clear
                    bookCategory.setText("");
                    bookTitle.setText("");
                    bookPress.setText("");
                    bookPublishYear.setText("");
                    bookAuthor.setText("");
                    bookPrice.setText("");
                    bookStock.setText("");
                    BookID.setText("");
                    refreshBook();
                }
            });

            queryButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    BookQueryConditions conditions = new BookQueryConditions();
                    if(!bookCategory.getText().isEmpty()){
                        String category = bookCategory.getText();
                        conditions.setCategory(category);
                    }
                    if(!bookTitle.getText().isEmpty()) {
                        String title = bookTitle.getText();
                        conditions.setTitle(title);
                    }
                    if(!bookPress.getText().isEmpty()) {
                        String press = bookPress.getText();
                        conditions.setPress(press);
                    }
                    if(!bookMinPublishYear.getText().isEmpty()){
                        int minPublishYear = Integer.parseInt(bookMinPublishYear.getText());
                        conditions.setMinPublishYear(minPublishYear);
                    }
                    if(!bookMaxPublishYear.getText().isEmpty()){
                        int maxPublishYear = Integer.parseInt(bookMaxPublishYear.getText());
                        conditions.setMaxPublishYear(maxPublishYear);
                    }
                    if(!bookMinPrice.getText().isEmpty()){
                        double minPrice = Double.parseDouble(bookMinPrice.getText());
                        conditions.setMinPrice(minPrice);
                    }
                    if(!bookMaxPrice.getText().isEmpty()){
                        double maxPrice = Double.parseDouble(bookMaxPrice.getText());
                        conditions.setMaxPrice(maxPrice);
                    }
                    if(!bookAuthor.getText().isEmpty()){
                        String author = bookAuthor.getText();
                        conditions.setAuthor(author);
                    }
                    if(sortOrderBox.getSelectedItem().toString().equals("升序")) {
                        conditions.setSortOrder(SortOrder.ASC);
                    } else if(sortOrderBox.getSelectedItem().toString().equals("降序")) {
                        conditions.setSortOrder(SortOrder.DESC);
                    }
                    if(sortOptions.getSelectedItem().toString().equals("Book ID")) {
                        conditions.setSortBy(Book.SortColumn.BOOK_ID);
                    } else if(sortOptions.getSelectedItem().toString().equals("Category")) {
                        conditions.setSortBy(Book.SortColumn.CATEGORY);
                    } else if(sortOptions.getSelectedItem().toString().equals("Title")) {
                        conditions.setSortBy(Book.SortColumn.TITLE);
                    } else if(sortOptions.getSelectedItem().toString().equals("Press")) {
                        conditions.setSortBy(Book.SortColumn.PRESS);
                    } else if(sortOptions.getSelectedItem().toString().equals("PublishYear")) {
                        conditions.setSortBy(Book.SortColumn.PUBLISH_YEAR);
                    } else if(sortOptions.getSelectedItem().toString().equals("Author")) {
                        conditions.setSortBy(Book.SortColumn.AUTHOR);
                    } else if(sortOptions.getSelectedItem().toString().equals("Price")) {
                        conditions.setSortBy(Book.SortColumn.PRICE);
                    }
                    ApiResult result = library.queryBook(conditions);
                    if(!result.ok) {
                        JOptionPane.showMessageDialog(storeBookPanel.this, result.message, "Query Failure", JOptionPane.ERROR_MESSAGE);
                    }
                    //clear
                    bookCategory.setText("");
                    bookTitle.setText("");
                    bookPress.setText("");
                    bookPublishYear.setText("");
                    bookAuthor.setText("");
                    bookPrice.setText("");
                    bookStock.setText("");
                    BookID.setText("");
                    bookMaxPrice.setText("");
                    bookMinPrice.setText("");
                    bookMaxPublishYear.setText("");
                    bookMinPublishYear.setText("");
                    refreshBook(conditions);
                }
            });
//            refreshBook();
        }
    }

    public class borrowBookPanel extends JPanel {
        public borrowBookPanel(JPanel cards) {
            setLayout(null);
            ActionListener returnToMainAction = new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    CardLayout cardLayout = (CardLayout) cards.getLayout();
                    cardLayout.show(cards, "main");
                }
            };

            ActionListener refreshAction = new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    refreshBorrow();
                }
            };

            JButton refreshButton = refreshButton("REFRESH", refreshAction);
            add(refreshButton);

            JLabel cardID = new JLabel("Card ID");
            cardID.setBounds(100, 100, 70, 30);
            add(cardID);
            JTextField cardText = new JTextField(50);
            cardText.setBounds(170, 100, 100, 30);
            add(cardText);

            JLabel bookID = new JLabel("Book ID");
            bookID.setBounds(100, 130, 70, 30);
            add(bookID);
            JTextField bookText = new JTextField(50);
            bookText.setBounds(170, 130, 100, 30);
            add(bookText);
            addBorrowTable(this);

            JButton showHistory = new JButton("Show History");
            showHistory.setBounds(100, 560, 170, 30);
            add(showHistory);

            JButton borrowBook = new JButton("Borrow Book");
            borrowBook.setBounds(100, 590, 170, 30);
            add(borrowBook);

            JButton returnBook = new JButton("Return Book");
            returnBook.setBounds(100, 620, 170, 30);
            add(returnBook);

            JButton returnMain = addReturnButton("RETURN", returnToMainAction);
            returnMain.setBounds(100, 650, 170, 30);
            add(returnMain);

            showHistory.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    int cardID = Integer.parseInt(cardText.getText());
                    ApiResult result = library.showBorrowHistory(cardID);
                    if(!result.ok) {
                        JOptionPane.showMessageDialog(borrowBookPanel.this, result.message, "Show History Failure", JOptionPane.ERROR_MESSAGE);
                    }
                    refreshBorrow(cardID);
                }
            });
            borrowBook.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    int cardID = Integer.parseInt(cardText.getText());
                    int bookID = Integer.parseInt(bookText.getText());

                    Borrow b0 = new Borrow(bookID, cardID);
                    b0.resetBorrowTime();
                    ApiResult result = library.borrowBook(b0);
                    if(!result.ok) {
                        JOptionPane.showMessageDialog(borrowBookPanel.this, result.message, "Borrow Book Failure", JOptionPane.ERROR_MESSAGE);
                    }

                    //clear
                    cardText.setText("");
                    bookText.setText("");
                    refreshBorrow();
                }
            });

            returnBook.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    int cardID = Integer.parseInt(cardText.getText());
                    int bookID = Integer.parseInt(bookText.getText());
                    Borrow r0 = new Borrow(bookID, cardID);
                    r0.resetReturnTime();
                    ApiResult result = library.returnBook(r0);
                    if (!result.ok) {
                        JOptionPane.showMessageDialog(borrowBookPanel.this, result.message, "Return Book Failure", JOptionPane.ERROR_MESSAGE);
                    }
                    //clear
                    cardText.setText("");
                    bookText.setText("");
                    refreshBorrow();
                }
            });

//            refreshBorrow();
        }
    }
    public static void main(String[] args) {
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception e) {
            // If Nimbus is not available, you can set the GUI to another look and feel.
        }

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new GUI();
            }
        });
    }
    private static JButton addReturnButton(String text, ActionListener actionListener) {
        JButton button = new JButton(text);
        button.addActionListener(actionListener);
//        button.setBounds(100, 100, 150, 30);
        return button;
    }

    private static JButton refreshButton(String text, ActionListener actionListener) {
        JButton button = new JButton(text);
        button.addActionListener(actionListener);
        button.setBounds(100, 30, 170, 30);
        return button;
    }
    private static void placeComponents(JPanel mainPanel, JPanel storeBook, JPanel borrowBook, JPanel registerCards, JPanel cards) {
        mainPanel.setLayout(null);

        JLabel Title = new JLabel("图书管理系统");
        Font font = new Font("微软雅黑", Font.PLAIN, 40);
        Title.setFont(font);
        Title.setSize(300, 300);
        Title.setLocation(480, 100);
        Title.setHorizontalTextPosition(SwingConstants.CENTER);
        mainPanel.add(Title);

        storeBook.setLayout(null);
        JButton storeBookButton = new JButton("Book");
        storeBookButton.setBounds(450, 300, 300, 50);
        mainPanel.add(storeBookButton);


        borrowBook.setLayout(null);
        JButton borrowBookButton = new JButton("Borrow");
        borrowBookButton.setBounds(450, 350, 300, 50);
        mainPanel.add(borrowBookButton);

        registerCards.setLayout(null);
        JButton registerButton = new JButton("Card");
        registerButton.setBounds(450, 400, 300, 50);
        mainPanel.add(registerButton);

        storeBookButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                CardLayout cardLayout = (CardLayout) cards.getLayout();
                cardLayout.show(cards, "storeBook");
            }
        });

        borrowBookButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                CardLayout cardLayout = (CardLayout) cards.getLayout();
                cardLayout.show(cards, "borrowBook");
            }
        });

        registerButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                CardLayout cardLayout = (CardLayout) cards.getLayout();
                cardLayout.show(cards, "registerBook");
            }
        });


    }

    private static void storeBookPanel(JPanel panel) {
        panel.setLayout(null);
        JButton storeButton = new JButton("storeBook");
        storeButton.setBounds(560, 140, 150, 30);
        panel.add(storeButton);
    }
    private static void borrowBookPanel(JPanel panel) {
        panel.setLayout(null);
        JButton storeButton = new JButton("borrowBook");
        storeButton.setBounds(560, 170, 150, 30);
        panel.add(storeButton);
    }

}