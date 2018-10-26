/*
 * Copyright (C) 2017 Lightbend Inc. <http://www.lightbend.com>
 */
package akka.cluster.bootstrap.demo;

import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.cluster.Cluster;
import akka.cluster.ClusterEvent;
import akka.cluster.MemberStatus;
import akka.http.javadsl.ConnectHttp;
import akka.http.javadsl.Http;
import akka.http.javadsl.model.StatusCodes;
import akka.http.javadsl.server.AllDirectives;
import akka.http.javadsl.server.Route;
import akka.management.AkkaManagement;
import akka.management.cluster.bootstrap.ClusterBootstrap;
import akka.stream.ActorMaterializer;
import akka.stream.Materializer;

public class DemoApp extends AllDirectives {

  DemoApp() {
    ActorSystem system = ActorSystem.create("Appka");

    Materializer mat = ActorMaterializer.create(system);
    Cluster cluster = Cluster.get(system);

    system.log().info("Started [" + system + "], cluster.selfAddress = " + cluster.selfAddress() + ")");

    //#start-akka-management
    AkkaManagement.get(system).start();
    //#start-akka-management
    ClusterBootstrap.get(system).start();

    cluster
      .subscribe(system.actorOf(Props.create(ClusterWatcher.class)), ClusterEvent.initialStateAsEvents(), ClusterEvent.ClusterDomainEvent.class);

    Route route = path("ready", () -> {
      if (cluster.selfMember().status() == MemberStatus.up()) {
        return complete(StatusCodes.OK);
      } else {
        return complete(StatusCodes.INTERNAL_SERVER_ERROR);
      }
    });
    Http.get(system).bindAndHandle(route.flow(system, mat), ConnectHttp.toHost("0.0.0.0", 8080), mat);

    cluster.registerOnMemberUp(() -> {
      system.log().info("Cluster member is up!");
    });
  }

  public static void main(String[] args) {
    new DemoApp();
  }
}

