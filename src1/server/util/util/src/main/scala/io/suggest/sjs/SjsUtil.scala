package io.suggest.sjs

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 30.11.18 17:59
  * Description: ScalaJS-утиль для server-side-кода.
  */
object SjsUtil {

  def fileName(fileNamePrefix: String, isProd: Boolean, suffix: String): String = {
    s"$fileNamePrefix-${if(isProd) "" else "fast"}opt$suffix.js"
  }

  /** Отрендерить скрипты.*/
  def jsScripts(path: String, fileNamePrefix: String, isProd: Boolean): Seq[String] = {
    // Каррирование, чтобы упростить код ниже
    val __fn = fileName( fileNamePrefix, isProd, _: String )

    val bundleFileName =  __fn( "-bundle" )
    if ( getClass.getResource( s"$path/$bundleFileName" ) != null ) {
      bundleFileName :: Nil
    } else {
      __fn( "-library" ) ::
      __fn( "-loader" ) ::
      __fn( "" ) ::
      Nil
    }
  }

}
