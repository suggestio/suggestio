package util

import play.api.Play.current
import io.suggest.util.SiobixFsStaticT

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 05.04.13 18:33
 * Description: работа с метаданными siobix, которые лежат в dfs.
 */

object SiobixFs extends SiobixFsStaticT {

  // Разрешить переопределять dfs-директорию через ключ конфига.
  // TODO нужно использовать override как-то.
  val siobixOutDir = current.configuration getString "siobix.dfs.dir" getOrElse siobix_out_dir

}
