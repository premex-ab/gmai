# Fixed useManagedAi() Usage Example

## The Problem
The original issue was that `useManagedAi()` was trying to find the `setupManagedAi` and `teardownManagedAi` tasks at configuration time, but these tasks are only created in the `afterEvaluate` block of the plugin, causing a timing issue.

## The Solution
I fixed the issue by using Gradle's `Provider` API to defer the task resolution until execution time:

```kotlin
fun Task.useManagedAi() {
    // Use task references with proper error handling
    dependsOn(project.provider {
        try {
            project.tasks.named("setupManagedAi")
        } catch (e: Exception) {
            project.logger.warn("setupManagedAi task not found for task ${name} - make sure the Gradle Managed AI plugin is applied")
            null
        }
    })
    
    finalizedBy(project.provider {
        try {
            project.tasks.named("teardownManagedAi")
        } catch (e: Exception) {
            project.logger.warn("teardownManagedAi task not found for task ${name} - make sure the Gradle Managed AI plugin is applied")
            null
        }
    })
}
```

## How to Use

In your `build.gradle.kts`:

```kotlin
plugins {
    id("se.premex.gmai")
}

// Import the extension function
import se.premex.gmai.plugin.utils.useManagedAi

managedAi {
    models {
        create("tinyllama") {
            version = "latest"
        }
    }
}

// Use with any task type
tasks.withType<Test> {
    useManagedAi()
}

// Use with custom tasks
tasks.register("myCustomTask") {
    useManagedAi()
    doLast {
        println("Task executed with managed AI available")
    }
}

// Use with custom configuration
tasks.register("myAdvancedTask") {
    useManagedAi {
        autoTeardown = false  // Don't auto-teardown after this task
    }
    doLast {
        println("Task executed with custom managed AI configuration")
    }
}
```

## What Was Fixed

1. **Timing Issue**: The original code tried to resolve tasks immediately, but they didn't exist yet during configuration phase
2. **Provider Pattern**: Now uses `project.provider {}` to defer task resolution until execution time
3. **Error Handling**: Gracefully handles cases where the plugin isn't applied or tasks don't exist
4. **Import Statement**: Added proper import statement so the extension function is available in build scripts

## Test Results
Both test cases now pass:
- ✅ `useManagedAi()` works correctly when the plugin is applied
- ✅ Graceful handling when the plugin is not applied

The fixed function now properly sets up task dependencies so that:
- Your task depends on `setupManagedAi` (starts Ollama and pulls models)
- Your task is finalized by `teardownManagedAi` (stops Ollama after completion)
