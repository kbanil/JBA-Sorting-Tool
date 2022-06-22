package sorting;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Collections.frequency;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toCollection;

enum DataType {
    LONG("long", "numbers"), LINE("line", "lines"), WORD("word", "words");

    private final String name;
    private final String printName;
    private static final Map<String, DataType> DATA_TYPES = new HashMap<>();

    DataType(String name, String printName) {
        this.name = name;
        this.printName = printName;
    }

    public String getName() {
        return name;
    }

    public String getPrintName() {
        return printName;
    }

    static {
        for (DataType dataType : DataType.values()) {
            DATA_TYPES.put(dataType.name, dataType);
        }
    }

    public static DataType parse(String name) {
        return DATA_TYPES.get(name);
    }
}

enum SortingType {
    NATURAL("natural"), BY_COUNT("byCount");
    private final String name;
    private static final Map<String, SortingType> SORTING_TYPES = new HashMap<>();

    public String getName() {
        return name;
    }

    SortingType(String name) {
        this.name = name;
    }

    static {
        for (SortingType sortingType : SortingType.values()) {
            SORTING_TYPES.put(sortingType.getName(), sortingType);
        }
    }

    public static SortingType parse(String name) {
        return SORTING_TYPES.get(name);
    }
}

class Arguments implements AutoCloseable {
    private final SortingType sortingType;
    private final DataType dataType;
    private final InputStream inputStream;
    private final PrintStream outputStream;

    Arguments(SortingType sortingType, DataType dataType, InputStream inputStream, PrintStream outputStream) {
        this.sortingType = sortingType;
        this.dataType = dataType;
        this.inputStream = inputStream;
        this.outputStream = outputStream;
    }

    public SortingType getSortingType() {
        return sortingType;
    }

    public DataType getDataType() {
        return dataType;
    }

    public InputStream getInputStream() {
        return inputStream;
    }

    public PrintStream getOutputStream() {
        return outputStream;
    }

    static Arguments parse(String[] args) throws FileNotFoundException {
        SortingType sortingType = SortingType.NATURAL;
        DataType dataType = DataType.WORD;
        InputStream inputStream = System.in;
        PrintStream outputStream = System.out;
        for (int i = 0; i < args.length; i++) {
            final String value = args[i];
            if ("-sortingType".equalsIgnoreCase(value)) {
                validate(args, i, "sorting type");
                sortingType = SortingType.parse(args[i + 1]);
            } else if ("-dataType".equalsIgnoreCase(value)) {
                validate(args, i, "data type");
                dataType = DataType.parse(args[i + 1]);
            } else if ("-inputFile".equalsIgnoreCase(value)) {
                validate(args, i, "input file");
                inputStream = new BufferedInputStream(new FileInputStream(args[i + 1]));
            } else if ("-outputFile".equalsIgnoreCase(value)) {
                validate(args, i, "output file");
                outputStream = new PrintStream(new BufferedOutputStream(new FileOutputStream(args[i + 1])));
            } else if (value.startsWith("-")) {
                System.err.printf("\"%s\" is not a valid parameter. It will be skipped.%n", value);
            }
        }
        return new Arguments(sortingType, dataType, inputStream, outputStream);
    }

    static void validate(String[] args, int index, String type) {
        final String errorMessage = "No " + type + " defined!";
        final int nextIndex = index + 1;
        if (nextIndex >= args.length) {
            throw new IllegalArgumentException(errorMessage);
        }
        if (args[nextIndex].startsWith("-")) {
            throw new IllegalArgumentException(errorMessage);
        }
    }

    @Override
    public void close() throws Exception {
        if (inputStream != System.in) {
            inputStream.close();
        }
        if (outputStream != System.out) {
            outputStream.close();
        }
    }
}

interface Sorter {
    void sortAndPrint(List<? extends Comparable<? super Comparable<?>>> input);
}

class NaturalOrderSorter implements Sorter {
    private final DataType dataType;
    private final PrintStream outputStream;
    private final String delimiter;

    NaturalOrderSorter(DataType dataType, PrintStream outputStream) {
        this.dataType = dataType;
        this.outputStream = outputStream;
        if (dataType == DataType.LINE) {
            this.delimiter = "\n";
        } else {
            this.delimiter = " ";
        }
    }

    @Override
    public void sortAndPrint(List<? extends Comparable<? super Comparable<?>>> input) {
        input.sort(Comparator.naturalOrder());
        print(input);
    }

    private void print(List<? extends Comparable<? super Comparable<?>>> input) {
        outputStream.printf("Total %s: %d.%n", dataType.getPrintName(), input.size());
        final String output = input.stream().map(Object::toString).collect(Collectors.joining(delimiter));
        outputStream.printf("Sorted data:%s%s%n", delimiter, output);
    }
}

class ByCountSorter implements Sorter {
    private final DataType dataType;
    private final PrintStream outputStream;

    ByCountSorter(DataType dataType, PrintStream outputStream) {
        this.dataType = dataType;
        this.outputStream = outputStream;
    }

    @Override
    public void sortAndPrint(List<? extends Comparable<? super Comparable<?>>> input) {
        final int size = input.size();
        outputStream.printf("Total %s: %d.%n", dataType.getPrintName(), size);
        final Map<Integer, ? extends Set<? extends Comparable<? super Comparable<?>>>> map =
                input.stream().collect(groupingBy(i -> frequency(input, i), toCollection(TreeSet::new)));
        final TreeSet<Integer> occurrenceKeys = new TreeSet<>(map.keySet());
        for (Integer key : occurrenceKeys) {
            final Set<? extends Comparable<? super Comparable<?>>> bucketList = map.get(key);
            for (Comparable<? super Comparable<?>> item : bucketList) {
                int percentage = (int) (((double) key / size) * 100);
                outputStream.printf("%s: %d time(s), %d%%%n", item, key, percentage);
            }
        }
    }
}

public class Main {
    public static void main(final String[] args) throws IOException {
        try (Arguments arguments = Arguments.parse(args)) {
            Scanner scanner = new Scanner(arguments.getInputStream());
            List<String> lines = new ArrayList<>();
            while (scanner.hasNextLine()) {
                lines.add(scanner.nextLine());
            }
            final DataType dataType = arguments.getDataType();
            final Parser parser = ParserFactory.newParser(dataType);
            final List<? extends Comparable<? super Comparable<?>>> input = parser.parse(lines);
            final Sorter sorter = newSorter(arguments);
            sorter.sortAndPrint(input);
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }

    static Sorter newSorter(Arguments arguments) {
        final SortingType sortingType = arguments.getSortingType();
        final DataType dataType = arguments.getDataType();
        if (sortingType == SortingType.BY_COUNT) {
            return new ByCountSorter(dataType, arguments.getOutputStream());
        } else {
            return new NaturalOrderSorter(dataType, arguments.getOutputStream());
        }
    }
}

interface Parser {
    List parse(List<String> lines);
}

class LineParser implements Parser {

    @Override
    public List parse(List<String> lines) {
        return lines;
    }
}

class LongParser implements Parser {
    @Override
    public List parse(List<String> lines) {
        List<Long> numbers = new ArrayList<>();
        for (String line : lines) {
            Scanner scanner = new Scanner(line);
            while (scanner.hasNext()) {
                String strValue = scanner.next();
                try {
                    final long longValue = Long.parseLong(strValue);
                    numbers.add(longValue);
                } catch (NumberFormatException numberFormatException) {
                    System.err.printf("\"%s\" is not a long. It will be skipped.%n", strValue);
                }
            }
        }
        return numbers;
    }
}

class WordParser implements Parser {
    @Override
    public List parse(List<String> lines) {
        List<String> words = new ArrayList<>();
        for (String line : lines) {
            Scanner scanner = new Scanner(line);
            while (scanner.hasNext()) {
                words.add(scanner.next());
            }
        }
        return words;
    }
}

class ParserFactory {
    static Parser newParser(DataType dataType) {
        switch (dataType) {
            case LINE:
                return new LineParser();
            case LONG:
                return new LongParser();
            case WORD:
                return new WordParser();
        }
        return null;
    }
}
