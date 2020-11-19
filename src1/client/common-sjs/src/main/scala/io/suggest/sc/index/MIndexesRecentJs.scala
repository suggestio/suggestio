package io.suggest.sc.index

import io.suggest.conf.ConfConst
import io.suggest.kv.MKvStorage

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 19.11.2020 17:09
  * Description: js-утиль для кросс-платформенной модели MIndexesRecent.
  */
object MIndexesRecentJs {

  /** Сохранить указанное состояние. */
  def save(li: MScIndexes): Unit = {
    MKvStorage.save(
      MKvStorage(
        key = ConfConst.SC_INDEXES_RECENT,
        value = li,
      )
    )
  }


  /** Прочитать сохранённое ранее значение. */
  def get(): Option[MScIndexes] = {
    MKvStorage
      .get[MScIndexes]( ConfConst.SC_INDEXES_RECENT )
      .map(_.value)
  }

}
