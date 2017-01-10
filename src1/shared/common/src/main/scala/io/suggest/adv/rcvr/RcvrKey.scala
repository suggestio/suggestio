package io.suggest.adv.rcvr

/** Ключ в карте текущих ресиверов. */
case class RcvrKey(from: String, to: String, groupId: Option[String]) {

  override def toString = from + "." + to + "." + groupId.getOrElse("")

}
