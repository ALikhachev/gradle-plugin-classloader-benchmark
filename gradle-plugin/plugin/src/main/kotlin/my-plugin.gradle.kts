import com.alikhachev.DoesSomething
import java.net.URLClassLoader
import java.util.*

abstract class DoSomething : DefaultTask() {
    @get:Inject
    abstract val workerExecutor: WorkerExecutor

    @get:Classpath
    abstract val workClasspath: ConfigurableFileCollection

    @get:Input
    abstract val useCustomClassloader: Property<Boolean>

    @get:Internal
    abstract val classLoaderProvider: Property<ClassLoaderProvider>

    @TaskAction
    fun doSomething() {
        if (useCustomClassloader.get()) {
            workerExecutor.noIsolation().submit(CustomClassLoaderIsolationDoSomethingAction::class.java) {
                classLoaderProvider.set(this@DoSomething.classLoaderProvider)
                classpath.set(workClasspath)
            }
        } else {
            workerExecutor.classLoaderIsolation {
                classpath.from(workClasspath)
            }.submit(ClasspathIsolationDoSomethingAction::class.java) {}
        }
    }
}

val workClasspathConfiguration = configurations.register("workClasspath")

abstract class ClasspathIsolationDoSomethingAction : WorkAction<WorkParameters.None> {
    override fun execute() {
        val doesSomething = ServiceLoader.load(DoesSomething::class.java).first()
        doesSomething.doSomething()
    }
}

abstract class ClassLoaderProvider : BuildService<BuildServiceParameters.None>, AutoCloseable {
    private val classLoaders = mutableMapOf<Iterable<File>, ClassLoader>()

    fun getClassLoader(classpath: Iterable<File>) = classLoaders.computeIfAbsent(classpath) {
        println("new classloader")
        URLClassLoader(
            classpath.toList().map { it.toURI().toURL() }.toTypedArray(),
            this.javaClass.classLoader
        )
    }

    override fun close() {
        classLoaders.clear()
    }
}

abstract class CustomClassLoaderIsolationDoSomethingAction :
    WorkAction<CustomClassLoaderIsolationDoSomethingAction.Parameters> {
    interface Parameters : WorkParameters {
        val classLoaderProvider: Property<ClassLoaderProvider>
        val classpath: ListProperty<File>
    }

    override fun execute() {
        val doesSomething =
            ServiceLoader.load(DoesSomething::class.java, parameters.classLoaderProvider.get().getClassLoader(parameters.classpath.get())).first()
        doesSomething.doSomething()
    }
}

val classLoaderProviderService =
    gradle.sharedServices.registerIfAbsent("class-loader-provider", ClassLoaderProvider::class.java) {

    }

tasks.withType<DoSomething> {
    workClasspath.from(workClasspathConfiguration)
    classLoaderProvider.set(classLoaderProviderService)
    usesService(classLoaderProviderService)
    useCustomClassloader.set(providers.gradleProperty("useCustomClassloader").map { it.toBoolean() }.orElse(false))
}
val doSomething0 by tasks.registering(DoSomething::class)
repeat(1000) { index ->
    tasks.register("doSomething${index + 1}", DoSomething::class.java) {
        dependsOn("doSomething${index}")
    }
}