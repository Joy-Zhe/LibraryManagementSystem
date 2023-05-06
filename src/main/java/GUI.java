import entities.Book;
import entities.Borrow;
import entities.Card;
import org.apache.commons.lang3.RandomUtils;
import queries.*;
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
            queryButton.setBounds(100, 430, 170, 30);
            add(queryButton);

            JButton bulkStoreBookButton = new JButton("Bulk Store Book");
            bulkStoreBookButton.setBounds(100, 600, 170, 30);
            add(bulkStoreBookButton);

            JButton returnMain = addReturnButton("RETURN", returnToMainAction);
            returnMain.setBounds(100, 650, 170, 30);
            add(returnMain);

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
                    if(!bookCategory.getText().equals("")){
                        String category = bookCategory.getText();
                        conditions.setCategory(category);
                    }
                    if(!bookTitle.getText().equals("")) {
                        String press = bookPress.getText();
                        conditions.setPress(press);
                    }
                    int publishYear = Integer.parseInt(bookPublishYear.getText());
                    String author = bookAuthor.getText();
                    double price = Double.parseDouble(bookPrice.getText());
                    int stock = Integer.parseInt(bookStock.getText());
                    int bookID = Integer.parseInt(BookID.getText());
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

            JButton borrowBook = new JButton("Borrow Book");
            borrowBook.setBounds(100, 590, 170, 30);
            add(borrowBook);

            JButton returnBook = new JButton("Return Book");
            returnBook.setBounds(100, 620, 170, 30);
            add(returnBook);

            JButton returnMain = addReturnButton("RETURN", returnToMainAction);
            returnMain.setBounds(100, 650, 170, 30);
            add(returnMain);

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

        storeBook.setLayout(null);
        JButton storeBookButton = new JButton("Book");
        storeBookButton.setBounds(450, 200, 300, 50);
        mainPanel.add(storeBookButton);


        borrowBook.setLayout(null);
        JButton borrowBookButton = new JButton("Borrow");
        borrowBookButton.setBounds(450, 250, 300, 50);
        mainPanel.add(borrowBookButton);

        registerCards.setLayout(null);
        JButton registerButton = new JButton("Card");
        registerButton.setBounds(450, 300, 300, 50);
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