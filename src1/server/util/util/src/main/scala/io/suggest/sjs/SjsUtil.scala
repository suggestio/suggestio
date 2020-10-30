package io.suggest.sjs

import io.suggest.common.empty.OptionUtil
import io.suggest.routes.RoutesJvmConst.ASSETS_PUBLIC_ROOT
import io.suggest.util.logs.MacroLogsDyn

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 30.11.18 17:59
  * Description: ScalaJS-утиль для server-side-кода.
  */
object SjsUtil extends MacroLogsDyn {

  def isDevelDetected: Boolean =
    _isFullOptDetected contains false

  private def fileName(fileNamePrefix: String, isProd: Boolean, suffix: String): String = {
    s"$fileNamePrefix-${if(isProd) "" else "fast"}opt$suffix.js"
  }

  /** Чтобы постоянно не дёргать проверку URL.getResource(), тут кэшируем результат. */
  private var _isFullOptDetected: Option[Boolean] = None


  /** Интерфейс для инфы по  */
  private sealed trait ISjsInfo extends Product {
    def FLAG: Boolean
    def FIRST_JS_FILE_SUFFIX: String
    def REST_JS_FILE_SUFFIX: Seq[String]
  }
  private case object Application extends ISjsInfo {
    override def FLAG = false
    override def FIRST_JS_FILE_SUFFIX = "-bundle"
    override def REST_JS_FILE_SUFFIX = Nil
  }
  private case object LibraryOnly extends ISjsInfo {
    override def FLAG = true
    override def FIRST_JS_FILE_SUFFIX = "-library"
    override def REST_JS_FILE_SUFFIX = "-loader" #:: "" #:: LazyList.empty
  }

  /** Кэш результатов по скриптам, которые могут быть LibraryOnly() или Application. */
  private var _isLibraryBundles: Map[String, ISjsInfo] = Map.empty


  def jsScripts(fileNamePrefix: String, playIsProd: Boolean): Seq[String] = {
    def __probeBundleModes(isProd2: Boolean): Seq[String] = {
      // Каррирование, чтобы укоротить код ниже.
      val __fn = fileName( fileNamePrefix, isProd2, _: String )

      _isLibraryBundles
        .get( fileNamePrefix )
        .fold {
          (LibraryOnly #:: Application #:: LazyList.empty[ISjsInfo])
            .iterator
            .flatMap { a =>
              val suffixedFileName = __fn( a.FIRST_JS_FILE_SUFFIX )
              Option.when( getClass.getResource( s"$ASSETS_PUBLIC_ROOT/$suffixedFileName" ) != null ) {
                // Кэшируем результат проверки во внутренней переменной: одна удачная проверка действительна и для всех последующих элементов.
                _isLibraryBundles += (fileNamePrefix -> a)
                LOGGER.trace(s"isLibraryBundles += $fileNamePrefix -> $a")
                // Кэшируем результат проверки в переменной: одна удачная проверка действительна и для всех последующих элементов.
                a -> suffixedFileName
              }
            }
            .nextOption()

        } { sjsInfo =>
          // Есть закэшированный результат bundle-пробы для данного js'ника. Сразу его и возвращаем.
          Some( sjsInfo -> __fn(sjsInfo.FIRST_JS_FILE_SUFFIX) )
        }
        // Сконвертировать результат проверки в список имён js-файлов:
        .fold[Seq[String]] (Nil) { case (a, firstFileName) =>
          // Добавить необходимые названия в хвост и текущее название - в начало.
          a.REST_JS_FILE_SUFFIX
            .map(__fn)
            .prepended( firstFileName )
        }
    }

    // Сначала определить, у нас fastOpt или fullOpt для данного скрипта...
    _isFullOptDetected.fold {
      // Ещё не закэширован фактический fullOpt-режим для js-файлов.
      (for {
        // isProd определяет приоритетность поиска по наиболее вероятному сценарию, но из-за
        // особенностей sbt-web-scalajs 1.1+ это так, как ожидается.
        isProd2 <- (playIsProd #:: !playIsProd #:: LazyList.empty)
        res = __probeBundleModes( isProd2 )
        if res.nonEmpty
      } yield {
        _isFullOptDetected = OptionUtil.SomeBool( isProd2 )
        LOGGER.trace(s"isFullOpt := $isProd2")
        res
      })
        .headOption
        .getOrElse( Nil )

    } {
      // fast/fullOpt-режим компиляции js-файлов уже закэширован. Перейти к определению Application/LibraryOnly-режимами
      // запрашиваемого js-ника:
      __probeBundleModes
    }
  }

}
