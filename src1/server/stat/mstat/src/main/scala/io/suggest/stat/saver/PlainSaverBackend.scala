package io.suggest.stat.saver

import io.suggest.stat.m.{MStat, MStats}
import org.elasticsearch.common.inject.Inject

import scala.concurrent.Future

/** Plain backend вызывает save() для всех элементов очереди. */
class PlainSaverBackend @Inject() (
  mStats                      : MStats
)
  extends StatSaverBackend
{

  override def save(stat: MStat): Future[_] = {
    mStats.save(stat)
  }

  override def flush(): Unit = {}

  override def close(): Future[_] = {
    Future.successful(None)
  }

}
