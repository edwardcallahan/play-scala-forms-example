import com.google.inject.AbstractModule
import play.api.libs.concurrent.AkkaGuiceSupport

class Module extends AbstractModule with AkkaGuiceSupport {
  import controllers.ProcessManagerActor

  override def configure(): Unit = {
    bindActor[ProcessManagerActor]("processManagerActor")
  }
}
