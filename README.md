[![badge license][badge-license]][home]
[![badge build][badge-build]][home]

# gsoc-hsearch
Example usages of Hibernate Search for GSoC (Google Summer of Code)


## Before started

These examples are created under Eclipse IDE with JDK 1.8. 

## Examples

### zoo-jpa

This is an example to demonstrate the how to build a web application integrating
Hibernate Search feature and JPA 2.1. If you want to run the demo, you should 
use MySQL database and import data from script `src/main/resources/zoo.sql`.

There're 3 types of Lucene query in the demo, they're

* keyword query
* Fuzzy search
* Wildcard search 

You can switch them by commenting / uncommenting lines 101 - 103 in class
`io.github.mincongh.servlet.AnimalSearchServlet`.

#### Keyword query

The most basic form of search. As the name suggests, this query type searches 
for one or more particular words.

```java
private Query keywordQuery(String searchString) {
    return queryBuilder
            .keyword()
            .onFields("name", "type")
            .matching(searchString)
            .createQuery();
}
```

All entities containing the target keyword(s) will be reached.

#### Fuzzy search

With a fuzzy search, keywords match against fields even when they are off by 
one or more characters. Check [wikipedia][wiki-ed] to know more about the 
_Edit Distance_.

```java
private Query fuzzyQuery(String searchString) {
    return queryBuilder
            .keyword()
            .fuzzy()
//          .withThreshold(0.7f)        // deprecated
//                                      // use withEditDistanceUpTo(int)
            .withEditDistanceUpTo(2)    // default 2 (can be 0, 1, 2)
            .onFields("name", "type")
            .matching(searchString)
            .createQuery();
}
```

#### Wildcard search

Lucene supports single and multiple character wildcard searches within single 
terms (not within phrase queries)

* To perform a single character wildcard search, user the `?` symbol
* To perform a multiple character wildcard search, user the `*` symbol

```java
private Query wildcardQuery(String searchString) {
    return queryBuilder
            .keyword()
            .wildcard()
            .onFields("name", "type")
            .matching(searchString)
            .createQuery();
}
```

[wiki-ed]: https://en.wikipedia.org/wiki/Edit_distance
[badge-build]: https://img.shields.io/badge/build-failed-red.svg
[badge-license]: https://img.shields.io/badge/license-Apache2.0-brightgreen.svg
[home]: https://github.com/mincong-h/gsoc-hsearch