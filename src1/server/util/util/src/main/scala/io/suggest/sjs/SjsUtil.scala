package io.suggest.sjs

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 30.11.18 17:59
  * Description: ScalaJS-утиль для server-side-кода.
  */
object SjsUtil {

  private def fileName(fileNamePrefix: String, isProd: Boolean, suffix: String): String = {
    s"$fileNamePrefix-${if(isProd) "" else "fast"}opt$suffix.js"
  }

  /** Отрендерить скрипты.*/
  def jsScripts(path: String, fileNamePrefix: String, isProd: Boolean): Seq[String] = {
    // isProd определяет приоритетность поиска по наиболее вероятному сценарию, но из-за
    // особенностей sbt-web-scalajs 1.1+ это так, как ожидается.
    (isProd #:: !isProd #:: LazyList.empty)
      .iterator
      .map { isProd2 =>
        // Каррирование, чтобы упростить код ниже
        val __fn = fileName( fileNamePrefix, isProd2, _: String )

        val bundleFileName =  __fn( "-bundle" )
        if ( getClass.getResource( s"$path/$bundleFileName" ) != null ) {
          bundleFileName :: Nil
        } else {
          val libFileName = __fn( "-library" )
          if ( getClass.getResource( s"$path/$libFileName" ) != null ) {
            libFileName ::
              __fn( "-loader" ) ::
              __fn( "" ) ::
              Nil
          } else {
            Nil
          }
        }
      }
      // Найти первый непустой список путей:
      .find( _.nonEmpty )
      .getOrElse {
        Nil
      }
  }

}
