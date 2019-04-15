package io.suggest.conf

import play.api.{ConfigLoader, Configuration}

import scala.util.Try

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 14.04.19 9:52
  * Description: Доп.утиль для typesafe config.
  */
object PlayConfigUtil {

  implicit class PlayConfigOpsExt(val conf: Configuration ) extends AnyVal {

    /** Взять из конфига опциональный массив, который может неявно объявлен одним элементом.
      * key.v = "asdf"
      * key.v = ["asdf"]
      *
      * @param key Ключ конфигурации.
      * @tparam T Тип одного значения.
      * @return None - нет такого элемента в конфиге.
      *         Some(Seq[T]) - есть ключ в конфиге.
      */
    def getOptionalSeq[T]( key: String )(implicit tCl: ConfigLoader[T], seqTCl: ConfigLoader[Seq[T]]): Option[Seq[T]] = {
      Try( conf.getOptional[T](key) )
        .map( _.map[Seq[T]](_ :: Nil) )
        .getOrElse {
          conf.getOptional[Seq[T]](key)
        }
    }

    /** Взять из конфига массив[T], который может неявно задан одним элементом [T].
      *
      * @param key Ключ конфигурации.
      * @tparam T Тип одного значения.
      * @return Найденный массив.
      * @throws NoSuchElementException Если нет такого ключа в конфиге.
      */
    def getSeq[T]( key: String )(implicit tCl: ConfigLoader[T], seqTCl: ConfigLoader[Seq[T]]): Seq[T] = {
      getOptionalSeq[T](key)
        .get
    }

    /** Взять из конфига массив[T], в т.ч. неявно заданный одним значением, или пустой массив,
      * когда нет такого элемента.
      *
      * @param key Ключ конфигурации.
      * @tparam T Тип одного значения.
      * @return Найденный массив значений или пустой массив.
      */
    def getSeqOrEmpty[T]( key: String )(implicit tCl: ConfigLoader[T], seqTCl: ConfigLoader[Seq[T]]): Seq[T] = {
      getOptionalSeq[T](key) getOrElse Nil
    }

  }

}
