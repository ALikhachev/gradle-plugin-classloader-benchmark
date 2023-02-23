includeBuild("../api") {
    dependencySubstitution {
        substitute(module("com.alikhachev.api:api")).using(project(":api"))
        substitute(module("com.alikhachev.api:impl")).using(project(":impl"))
    }
}
include(":plugin")