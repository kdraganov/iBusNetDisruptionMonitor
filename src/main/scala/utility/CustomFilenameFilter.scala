package utility

import java.io.{File, FilenameFilter}

/**
 * Created by Konstantin on 17/02/2015.
 */
class CustomFilenameFilter(private val prefix: String, private val suffix: String) extends FilenameFilter {

  def accept(dir: File, name: String): Boolean = {
    if (name.startsWith(prefix) && name.endsWith(suffix)) {
      return true
    }
    return false
  }
}
