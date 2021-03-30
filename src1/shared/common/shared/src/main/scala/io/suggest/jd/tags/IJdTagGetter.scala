package io.suggest.jd.tags

import io.suggest.jd.MJdTagId

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 14.11.2019 15:34
  * Description: typeclass-интерфейс для извлекателя JdTag из произвольных типов данных.
  */
sealed trait IJdTagGetter[From] extends (From => JdTag)


object IJdTagGetter {

  /** Сам себе тег. */
  implicit def Identity: IJdTagGetter[JdTag] = new IJdTagGetter[JdTag] {
    override def apply(from: JdTag) = from
  }

  /** Тег в связке с jd-id. */
  implicit def JdIdWithTag: IJdTagGetter[(MJdTagId, JdTag)] = new IJdTagGetter[(MJdTagId, JdTag)] {
    override def apply(from: (MJdTagId, JdTag)) = from._2
  }

  implicit def IndexedTag[A]: IJdTagGetter[(JdTag, A)] = new IJdTagGetter[(JdTag, A)] {
    override def apply(from: (JdTag, A)) = from._1
  }

}
