package part3.exercise.stream;

import data.Employee;
import data.JobHistoryEntry;
import data.Person;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.junit.Test;

import java.util.*;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.Stream;

import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.*;
import static org.junit.Assert.assertEquals;

public class StreamsExercise {

    @Test
    public void getAllJobHistoryEntries() {
        final List<Employee> employees = getEmployees();

        final List<JobHistoryEntry> jobHistoryEntries =
                employees.stream()
                        .flatMap(e -> e.getJobHistory().stream())
                        .collect(toList());

        assertEquals(22, jobHistoryEntries.size());
    }

    @Test
    public void getSumDuration() {
        final List<Employee> employees = getEmployees();

        final int sumDurations = employees.stream()
                .flatMap(e -> e.getJobHistory().stream())
                .mapToInt(JobHistoryEntry::getDuration)
                .sum();

        assertEquals(72, sumDurations);
    }

    private static class PersonEmployer {
        private final Person person;
        private final String employer;

        public PersonEmployer(Person person, String employer) {
            this.person = person;
            this.employer = employer;
        }

        public Person getPerson() {
            return person;
        }

        public String getEmployer() {
            return employer;
        }

        @Override
        public String toString() {
            return new ToStringBuilder(this)
                    .append("person", person)
                    .append("employer", employer)
                    .toString();
        }
    }

    @Test
    public void indexPersonsByEmployer1() {
        final List<Employee> employees = getEmployees();

        final Map<String, List<PersonEmployer>> index = employees.stream()
                .flatMap(this::getPersonEmployerPair)
                .collect(groupingBy(PersonEmployer::getEmployer));

        assertEquals(11, index.get("epam").size());
    }

    private Stream<PersonEmployer> getPersonEmployerPair(Employee employee) {
        return employee.getJobHistory().stream()
                .map(j -> new PersonEmployer(employee.getPerson(), j.getEmployer()));
    }

    @Test
    public void indexPersonsByEmployer2() {
        final List<Employee> employees = getEmployees();

        final Map<String, List<Person>> index = employees.stream()
                .flatMap(this::getPersonEmployerPair)
                .collect(groupingBy(
                        PersonEmployer::getEmployer,
                        mapping(PersonEmployer::getPerson,
                                toList())
                ));

        assertEquals(11, index.get("epam").size());
    }

    private static class PersonDuration {
        private final Person person;
        private final int duration;

        public PersonDuration(Person person, int duration) {
            this.person = person;
            this.duration = duration;
        }

        public Person getPerson() {
            return person;
        }

        public int getDuration() {
            return duration;
        }

        @Override
        public String toString() {
            return new ToStringBuilder(this)
                    .append("person", person)
                    .append("duration", duration)
                    .toString();
        }
    }

    private PersonDuration sumAllPersonDurations(Employee e) {
        int duration = e.getJobHistory().stream().mapToInt(JobHistoryEntry::getDuration).sum();
        return new PersonDuration(e.getPerson(), duration);
    }

    @Test
    public void getSumPersonDuration() {
        final List<Employee> employees = getEmployees();

        final Map<Person, Integer> personDuration = employees.stream()
                .map(this::sumAllPersonDurations)
                .collect(toMap(PersonDuration::getPerson, PersonDuration::getDuration));


        assertEquals(Integer.valueOf(8), personDuration.get(new Person("John", "Doe", 24)));
    }

    private static class PersonPositionIndex {
        private final Person person;
        private final Map<String, Integer> durationByPositionIndex;

        public PersonPositionIndex(Person person, Map<String, Integer> durationByPositionIndex) {
            this.person = person;
            this.durationByPositionIndex = durationByPositionIndex;
        }

        public Person getPerson() {
            return person;
        }

        public Map<String, Integer> getDurationByPositionIndex() {
            return durationByPositionIndex;
        }
    }

    private static PersonPositionIndex getPersonPositionIndex(Employee e) {
        Map<String, Integer> durationByPositionIndex = e.getJobHistory().stream()
                .collect(groupingBy(
                        JobHistoryEntry::getPosition,
                        summingInt(JobHistoryEntry::getDuration)
                ));

        return new PersonPositionIndex(e.getPerson(), durationByPositionIndex);
    }

    @Test
    public void getSumDurationsForPersonByPosition() {
        final List<Employee> employees = getEmployees();

        final List<PersonPositionIndex> personIndexes = employees.stream()
                .map(StreamsExercise::getPersonPositionIndex).collect(toList());

        assertEquals(1, personIndexes.get(3).getDurationByPositionIndex().size());
    }

    private static class PersonPositionDuration {
        private final Person person;
        private final String position;
        private final int duration;

        public PersonPositionDuration(Person person, String position, int duration) {
            this.person = person;
            this.position = position;
            this.duration = duration;
        }

        public Person getPerson() {
            return person;
        }

        public String getPosition() {
            return position;
        }

        public int getDuration() {
            return duration;
        }
    }

    @Test
    public void getDurationsForEachPersonByPosition() {
        final List<Employee> employees = getEmployees();

        final List<PersonPositionDuration> personPositionDuration = employees.stream()
                .flatMap(this::getPersonPositionDuration)
                .collect(toList());

        assertEquals(17, personPositionDuration.size());
    }

    private Stream<PersonPositionDuration> getPersonPositionDurations(Employee employee) {
        return employee.getJobHistory().stream()
                .map(j -> new PersonPositionDuration(employee.getPerson(), j.getPosition(), j.getDuration()));
    }

    private Stream<PersonPositionDuration> getPersonPositionDuration(Employee employee) {
        return employee.getJobHistory().stream()
                .collect(groupingBy(JobHistoryEntry::getPosition,
                        summingInt(JobHistoryEntry::getDuration)))
                .entrySet().stream()
                .map(e -> new PersonPositionDuration(employee.getPerson(),
                        e.getKey(),
                        e.getValue()));
    }

    @Test
    public void getCoolestPersonByPosition1() {
        final List<Employee> employees = getEmployees();

        final Map<String, PersonPositionDuration> coolestPersonByPosition = employees.stream()
                .flatMap(this::getPersonPositionDurations)
                .collect(toMap(PersonPositionDuration::getPosition,
                        Function.identity(),
                        BinaryOperator.maxBy(comparing(PersonPositionDuration::getDuration))));


        assertEquals(new Person("John", "White", 22), coolestPersonByPosition.get("QA").getPerson());
    }

    @Test
    public void getCoolestPersonByPosition2() {
        final List<Employee> employees = getEmployees();

        final Map<String, Person> coolestPersonByPosition = employees.stream()
                .flatMap(this::getPersonPositionDurations)
                .collect(groupingBy(PersonPositionDuration::getPosition,
                        collectingAndThen(
                                maxBy(comparing(
                                        PersonPositionDuration::getDuration
                                )),
                                ppd -> ppd.get().getPerson()
                        )));


        assertEquals(new Person("John", "White", 22), coolestPersonByPosition.get("QA"));
    }

    private List<Employee> getEmployees() {
        return Arrays.asList(
                new Employee(
                        new Person("John", "Galt", 20),
                        Collections.emptyList()),
                new Employee(
                        new Person("John", "Doe", 21),
                        Arrays.asList(
                                new JobHistoryEntry(4, "BA", "yandex"),
                                new JobHistoryEntry(2, "QA", "epam"),
                                new JobHistoryEntry(2, "dev", "abc")
                        )),
                new Employee(
                        new Person("John", "White", 22),
                        Collections.singletonList(
                                new JobHistoryEntry(6, "QA", "epam")
                        )),
                new Employee(
                        new Person("John", "Galt", 23),
                        Arrays.asList(
                                new JobHistoryEntry(3, "dev", "epam"),
                                new JobHistoryEntry(2, "dev", "google")
                        )),
                new Employee(
                        new Person("John", "Doe", 24),
                        Arrays.asList(
                                new JobHistoryEntry(4, "QA", "yandex"),
                                new JobHistoryEntry(2, "BA", "epam"),
                                new JobHistoryEntry(2, "dev", "abc")
                        )),
                new Employee(
                        new Person("John", "White", 25),
                        Collections.singletonList(
                                new JobHistoryEntry(6, "QA", "epam")
                        )),
                new Employee(
                        new Person("John", "Galt", 26),
                        Arrays.asList(
                                new JobHistoryEntry(3, "dev", "epam"),
                                new JobHistoryEntry(1, "dev", "google")
                        )),
                new Employee(
                        new Person("Bob", "Doe", 27),
                        Arrays.asList(
                                new JobHistoryEntry(4, "QA", "yandex"),
                                new JobHistoryEntry(2, "QA", "epam"),
                                new JobHistoryEntry(2, "dev", "abc")
                        )),
                new Employee(
                        new Person("John", "White", 28),
                        Collections.singletonList(
                                new JobHistoryEntry(6, "BA", "epam")
                        )),
                new Employee(
                        new Person("John", "Galt", 29),
                        Arrays.asList(
                                new JobHistoryEntry(3, "dev", "epam"),
                                new JobHistoryEntry(1, "dev", "google")
                        )),
                new Employee(
                        new Person("John", "Doe", 30),
                        Arrays.asList(
                                new JobHistoryEntry(4, "QA", "yandex"),
                                new JobHistoryEntry(2, "QA", "epam"),
                                new JobHistoryEntry(5, "dev", "abc")
                        )),
                new Employee(
                        new Person("Bob", "White", 31),
                        Collections.singletonList(
                                new JobHistoryEntry(6, "QA", "epam")
                        ))
        );
    }


}
