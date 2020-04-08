package flashcards;

import java.io.*;
import java.util.*;

public class Main {
    public static void main(String[] args) {
        FlashCards flashCards = new FlashCards();
        flashCards.start();
    }
}

class FlashCards {
    private Map<String, String> cards;
    private LogScanner scanner;
    private Random random;
    private List<String> lines;
    private PrintStream console;

    public FlashCards() {
        this.cards = new HashMap<>();
        this.scanner = new LogScanner(System.in);
        this.random = new Random();
        this.lines = new ArrayList<>();
        this.console = new PrintStream(System.out) {
            @Override
            public void println(String line) {
                super.println(line);
                lines.add(line);
            }

            @Override
            public PrintStream printf(String format, Object... args) {
                lines.add(String.format(format, args));
                return super.printf(format, args);
            }
        };
    }

    public void start() {
        boolean isStopped = false;
        while (!isStopped) {
            console.println("Input the action (add, remove, import, export, ask, exit):");
            String action = scanner.nextLine();
            switch (action) {
                case "add":
                    addNewCard();
                    break;
                case "remove":
                    removeCard();
                    break;
                case "import":
                    importCards();
                    break;
                case "export":
                    exportCards();
                    break;
                case "ask":
                    asking();
                    break;
                case "log":
                    log();
                    break;
                case "exit":
                    isStopped = true;
                    console.println("Bye bye!");
                    break;
                default:
                    console.printf("\"%s\" is unknown action. Please, try again...", action);
            }
        }
    }

    private void addNewCard() {
        console.println("The card:");
        String term = scanner.nextLine();
        if (!cards.containsKey(term)) {
            console.println("The definition of the card");
            String def = scanner.nextLine();
            if (!cards.containsValue(def)) {
                cards.put(term, def);
                console.printf("The pair (\"%s\":\"%s\") has been added.\n", term, def);
            } else {
                console.printf("The definition \"%s\" already exists.\n", def);
            }
        } else {
            console.printf("The card \"%s\" already exists.\n", term);
        }
    }

    private void removeCard() {
        console.println("The card:");
        String term = scanner.nextLine();
        if (cards.containsKey(term)) {
            cards.remove(term);
            console.println("The card has been removed.");
        } else {
            console.printf("Can't remove \"%s\": there is no such card.\n", term);
        }
    }

    private void importCards() {
        console.println("File name:");
        String fileName = scanner.nextLine();

        try (var fileScanner = new Scanner(new FileReader(fileName))) {
            int count = 0;
            while (fileScanner.hasNextLine()) {
                String term = fileScanner.nextLine();
                if (fileScanner.hasNextLine()) {
                    String def = fileScanner.nextLine();
                    cards.put(term, def);
                    count++;
                } else {
                    throw new IOException("The file are corrupted.");
                }
            }
            console.printf("%d cards have been loaded.\n", count);
        } catch (FileNotFoundException e) {
            console.println("File not found.");
        } catch (IOException e) {
            System.err.println(e.getMessage());
        }
    }

    private void exportCards() {
        console.println("File name:");
        String fileName = scanner.nextLine();

        try (var fileWriter = new PrintStream(new FileOutputStream(fileName, false))) {
            int count = 0;
            for (Map.Entry<String, String> card : cards.entrySet()) {
                fileWriter.println(card.getKey());
                fileWriter.println(card.getValue());
                count++;
            }
            console.printf("%d cards have been saved.\n", count);
        } catch (FileNotFoundException e) {
            System.err.println(e.getMessage());
        }
    }

    private void log() {
        console.println("File name:");
        String fileName = scanner.nextLine();

        try (var fileWriter = new PrintStream(new FileOutputStream(fileName, false))) {
            lines.forEach(fileWriter::println);
            console.println("The log has been saved.");
        } catch (FileNotFoundException e) {
            System.err.println(e.getMessage());
        }
    }

    private void asking() {
        if (!cards.isEmpty()) {
            ArrayList<String> termsList = new ArrayList<>(cards.keySet());
            console.println("How many times to ask?");
            int askTimes = Integer.parseInt(scanner.nextLine());
            for (int i = 0; i < askTimes; i++) {
                String term = termsList.get(random.nextInt(termsList.size()));
                String def = cards.get(term);
                console.printf("Print the definition of \"%s\":\n", term);
                String userDef = scanner.nextLine();
                if (userDef.equalsIgnoreCase(def)) {
                    console.println("Correct answer");
                } else if (cards.containsValue(userDef)) {
                    String correspondTerm = cards.entrySet().stream()
                            .filter(e -> userDef.equalsIgnoreCase(e.getValue()))
                            .findAny().get().getKey();
                    console.printf("Wrong answer. The correct one is \"%s\", " +
                                    "you've just written the definition of \"%s\".\n",
                            def, correspondTerm
                    );
                } else {
                    console.printf("Wrong answer. The correct one is \"%s\".\n", def);
                }
            }
        } else {
            console.println("Cards map are empty. Please, add some cards or import from  file.");
        }
    }

    private class LogScanner {
        private Scanner scanner;

        public LogScanner(InputStream inputStream) {
            scanner = new Scanner(inputStream);
        }

        public String nextLine() {
            String line = scanner.nextLine();
            lines.add(line);
            return line;
        }
    }
}