package io.suggest.di

import akka.actor.ActorSystem

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 07.12.15 18:12
 * Description: Интерфейс для DI-поля для доступа к инстансу ActorSystem.
 */
trait IActorSystem {
  def actorSystem: ActorSystem
}
