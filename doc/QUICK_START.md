# 🚀 Quick Start

> Spring 프레임워크를 사용하지 않는 경우 POJO의 예시를 봐주세요.

- [Gradle 설정](#gradle-설정)
  - [Kotlin DSL](#kotlin-dsl)
  - [Groovy](#groovy)
  - [Maven](#maven)
- [적용 방법](#적용-방법)
  - [Kotlin](#kotlin)
  - [Java](#java)
- [설정 중 발생할 수 있는 이슈](#설정-중-발생할-수-있는-이슈)

## Gradle 설정

### Kotlin DSL

**settings.gradle.kts**

```kotlin
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}
```

**build.gradle.kts**

```kotlin
dependencies {
    implementation("com.github.Jwhyee:profanity-filter:1.0.2")
}
```

### Groovy

**settings.gradle**

```groovy
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
        maven { url 'https://jitpack.io' }
    }
}
```

**build.gradle**

```groovy
dependencies {
    implementation 'com.github.Jwhyee:profanity-filter:1.0.2'
}
```

### Maven

**pom.xml**

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>

<dependencies>
    <dependency>
        <groupId>com.github.Jwhyee</groupId>
        <artifactId>profanity-filter</artifactId>
        <version>1.0.2</version>
    </dependency>
</dependencies>
```

## 적용 방법

`ProfanityTrie.create` 호출 시 두 가지 인자를 넘겨줄 수 있습니다. 이 인자에는 [제공된 비속어 목록](/src/main/kotlin/io/github/jwhyee/profanity/policy/ProfanityPolicy.kt)에 비속어를 더 추가(`customWords`) 하거나, 이미 존재하는 비속어를 제외(`excludeWords`)할 수 있습니다. 

만약, "시발"은 허용하지 않지만, "시발점"은 허용하고 싶은 경우와 같이 비속어로 처리하고 싶지 않은 단어가 있는 경우, `ProfanityValidator` 생성자의 두 번째 매개 변수인 `allowWords`에 추가해주시면 됩니다. 

### Kotlin

**Spring Configuration**

```kotlin
@Configuration
class FilterConfig {
    @Bean
    fun profanityValidator(): ProfanityValidator {
        val trie = ProfanityTrie.create(
            customWords = listOf("추가할 비속어"),
            excludeWords = listOf("제외할 비속어")
        )
        
        return ProfanityValidator(trie, setOf("시발점"))
    }
}
```

```kotlin
@Service
class FilterService(
    private val profanityValidator: ProfanityValidator
) {
    fun check(keyword: String) {
        try {
            validator.validate(keyword)
        } catch (e: ProfanityDetectedException) {
            log.error("비속어 발견: {}", e.detected)
        }
    }
}
```

**POJO**

```kotlin
class MyService {
    private val validator = ProfanityValidator(
        ProfanityTrie.create(),
        setOf("시발점")
    )

    fun check(text: String) {
        try {
            validator.validate(text)
        } catch (e: ProfanityDetectedException) {
            ...
        }
    }
}
```

### Java

**Spring Configuration**

```java
import io.github.jwhyee.profanity.helper.ProfanityTrie;
import io.github.jwhyee.profanity.validator.ProfanityValidator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.Set;

@Configuration
public class FilterConfig {
    @Bean
    public ProfanityValidator profanityValidator() {
        var trie = ProfanityTrie.create(
            List.of("추가할 비속어"),
            List.of("제외할 비속어")
        );
        
        return new ProfanityValidator(trie, Set.of("시발점"));
    }
}
```

```java
@Service
public class FilterService {
    private final ProfanityValidator validator;

    public MyService(ProfanityValidator validator) {
        this.validator = validator;
    }
    
    public void check(String keyword) {
        try {
            validator.validate(keyword);
        } catch (ProfanityDetectedException e) {
            System.out.println(e);
        }
    }
}
```

**POJO**

```java
import io.github.jwhyee.profanity.helper.ProfanityTrie;
import io.github.jwhyee.profanity.validator.ProfanityValidator;
import io.github.jwhyee.profanity.validator.ProfanityDetectedException;
import java.util.Set;

public class MyService {
    private static final ProfanityValidator VALIDATOR = new ProfanityValidator(
        ProfanityTrie.create(),
        Set.of("시발점")
    );

    public void check(String text) {
        try {
            VALIDATOR.validate(text);
        } catch (ProfanityDetectedException e) {
            System.out.println("감지된 비속어: " + e.getDetectedWords());
        }
    }
}
```

## 설정 중 발생할 수 있는 이슈

### Maven Repository 충돌

```
Build was configured to prefer settings repositories over project repositories
```

**해결 방법**: `build.gradle(.kts)`의 `repositories` 블록을 제거하고 `settings.gradle(.kts)`에만 유지하세요.

### Dependency 충돌

이 라이브러리는 내부적으로 `ahocorasick` 의존성을 포함합니다. 프로젝트에서 직접 추가한 `ahocorasick` 의존성이 있다면 제거해주세요.