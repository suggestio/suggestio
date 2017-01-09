package io.suggest.di

import akka.actor.ActorSystem
import io.suggest.playx.ICurrentApp

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 07.12.15 18:12
 * Description: Интерфейс для DI-поля для доступа к инстансу ActorSystem.
 */
trait IActorSystem {
  implicit def actorSystem: ActorSystem
}
trait ICurrentActorSystem extends IActorSystem with ICurrentApp {
  override implicit def actorSystem = current.actorSystem
}