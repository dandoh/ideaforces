package org.dandoh.ideaforces.core


sealed class Executor {

}


object CPPExecutor : Executor() {}

object JavaExecutor : Executor() {}

object PythonExecutor : Executor() {}


fun file2Executor(extension: String): Executor? {
  return when (extension) {
    "cc", "cpp" -> CPPExecutor
    "java" -> JavaExecutor
    "python" -> PythonExecutor
    else -> null
  }
}

