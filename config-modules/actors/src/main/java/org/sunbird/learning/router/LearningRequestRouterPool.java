package org.sunbird.learning.router;

import java.util.concurrent.TimeUnit;

import org.sunbird.common.exception.ServerException;
import org.sunbird.common.router.RequestRouterPool;
import org.sunbird.learning.common.enums.LearningErrorCodes;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.routing.SmallestMailboxPool;
import akka.util.Timeout;
import scala.concurrent.duration.Duration;

// TODO: Auto-generated Javadoc
/**
 * The Class LearningRequestRouterPool, provides functionality initialize actor
 * system for learning system
 *
 * @author karthik
 */
public class LearningRequestRouterPool {

	/** The actor. */
	private static ActorRef actor;

	/** The count. */
	private static int count = 5;

	/** The req timeout. */
	public static long REQ_TIMEOUT = 30000;

	/** The wait timeout. */
	public static Timeout WAIT_TIMEOUT = new Timeout(Duration.create(30, TimeUnit.SECONDS));

	/**
	 * Inits the.
	 */
	public static void init() {
		ActorSystem system = RequestRouterPool.getActorSystem();
		Props actorProps = Props.create(LearningRequestRouter.class);
		actor = system.actorOf(new SmallestMailboxPool(count).props(actorProps));
		actor.tell("init", system.deadLetters());
	}

	/**
	 * Gets the request router.
	 *
	 * @return the request router
	 */
	public static ActorRef getRequestRouter() {
		if (null == actor) {
			throw new ServerException(LearningErrorCodes.ERR_LEARNING_REQUEST_ROUTER_NOT_FOUND.name(),
					"Request Router not found");
		}
		return actor;
	}

	/**
	 * Destroy.
	 */
	public static void destroy() {
		actor = null;
	}
}
