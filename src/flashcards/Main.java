package flashcards;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

public class Main {
    public static void main(String[] args) {
        FlashCards flashCards = new FlashCards(args);
        flashCards.start();
    }
}

class FlashCards {
    private Map<String, String> cards;
    private Map<String, Integer> mistakes;
    private LogScanner scanner;
    private Random random;
    private List<String> lines;
    private PrintStream console;
    private String inputFileName;
    private String outputFileName;
    private boolean inputDefined;
    private boolean outputDefined;

    public FlashCards() {
        this.cards = new HashMap<>();
        this.mistakes = new HashMap<>();
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

    public FlashCards(String[] args) {
        this();
        for (int i = 0; i < args.length; i++) {
            if (args[i].equalsIgnoreCase("-import")) {
                if (i + 1 < args.length) {
                    inputFileName = args[i + 1];
                    inputDefined = true;
                    i++;
                }
            } else if (args[i].equalsIgnoreCase("-export")) {
                if (i + 1 < args.length) {
                    outputFileName = args[i + 1];
                    outputDefined = true;
                }
            }
        }
    }

    public void start() {
        if (inputDefined) {
            importCards(inputFileName);
        }
        boolean isStopped = false;
        while (!isStopped) {
            console.println(
                    "Input the action (add, remove, import, export, ask, exit," +
                            " log, hardest card, reset stats):"
            );
            String action = scanner.nextLine();
            switch (action) {
                case "add":
                    addNewCard();
                    break;
                case "remove":
                    removeCard();
                    break;
                case "import":
                    console.println("File name:");
                    importCards(scanner.nextLine());
                    break;
                case "export":
                    console.println("File name:");
                    exportCards(scanner.nextLine());
                    break;
                case "ask":
                    asking();
                    break;
                case "log":
                    log();
                    break;
                case "hardest card":
                    printHardestCards();
                    break;
                case "reset stats":
                    resetMistakesStat();
                    break;
                case "list":
                    list();
                    break;
                case "exit":
                    isStopped = true;
                    console.println("Bye bye!");
                    if (outputDefined) {
                        exportCards(outputFileName);
                    }
                    break;
                default:
                    console.printf("\"%s\" is unknown action. Please, try again...\n", action);
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
                mistakes.put(term, 0);
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
            mistakes.remove(term);
            console.println("The card has been removed.");
        } else {
            console.printf("Can't remove \"%s\": there is no such card.\n", term);
        }
    }

    private void importCards(String fileName) {
        try (var fileScanner = new Scanner(new FileReader(fileName))) {
            int count = 0;
            while (fileScanner.hasNextLine()) {
                String term = fileScanner.nextLine();
                String def = fileScanner.nextLine();
                cards.put(term, def);
                int mistakesCount = Integer.parseInt(fileScanner.nextLine());
                mistakes.put(term, mistakesCount);
                count++;
            }
            console.printf("%d cards have been loaded.\n", count);
        } catch (FileNotFoundException e) {
            console.println("File not found.");
        }
    }

    private void exportCards(String fileName) {
        try (var fileWriter = new PrintStream(new FileOutputStream(fileName, false))) {
            int count = 0;
            for (Map.Entry<String, String> card : cards.entrySet()) {
                fileWriter.println(card.getKey());
                fileWriter.println(card.getValue());
                fileWriter.println(mistakes.get(card.getKey()));
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

    private void resetMistakesStat() {
        mistakes.entrySet().forEach(entry -> entry.setValue(0));
        System.out.println("Card statistics has been reset.");
    }

    private void printHardestCards() {
        if (mistakes.size() > 0) {
            int max = Collections.max(mistakes.values());
            var hardestCards = mistakes.entrySet().stream()
                    .filter(entry -> entry.getValue() == max)
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());

            if (max == 0 || hardestCards.size() == 0) {
                System.out.println("There are no cards with errors.");
            } else if (hardestCards.size() == 1) {
                System.out.printf(
                        "The hardest card is \"%s\". You have %d errors answering it.\n",
                        hardestCards.get(0), mistakes.get(hardestCards.get(0))
                );
            } else {
                StringJoiner joiner = new StringJoiner(", ");
                hardestCards.forEach(term -> joiner.add(String.format("\"%s\"", term)));
                System.out.printf(
                        "The hardest cards are %s. You have %d errors answering them.\n",
                        joiner.toString(), max
                );
            }
        } else {
            System.out.println("There are no cards with errors.");
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
                            .findAny().orElseThrow().getKey();
                    mistakes.put(term, mistakes.get(term) + 1);
                    console.printf("Wrong answer. (The correct one is \"%s\", " +
                                    "you've just written the definition of \"%s\".)\n",
                            def, correspondTerm
                    );
                } else {
                    mistakes.put(term, mistakes.get(term) + 1);
                    console.printf("Wrong answer. (The correct one is \"%s\".)\n", def);
                }
            }
        } else {
            console.println("Cards map are empty. Please, add some cards or import from  file.");
        }
    }

    private void list() {
        cards.forEach(
                (term, def) -> {
                    System.out.printf("[%s] : [%s] : [%d] mistakes\n", term, def, mistakes.get(term));
                }
        );
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