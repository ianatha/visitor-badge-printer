import sbt._
import Keys._
import play.Project._
import com.github.retronym.SbtOneJar._
import com.github.retronym.SbtOneJar.oneJar
import com.typesafe.sbt.SbtNativePackager
import com.typesafe.sbt.SbtNativePackager._
import com.typesafe.sbt.packager.SettingsHelper.makeDeploymentSettings
import com.typesafe.sbt.packager.Keys._

object ApplicationBuild extends Build {

  val appName         = "moat"
  val appVersion      = "1.0-SNAPSHOT"

  val appDependencies = Seq(
    // Add your project dependencies here,
     jdbc
    ,anorm
    ,"com.typesafe.slick" %% "slick" % "1.0.0"
    ,"com.github.tototoshi" %% "slick-joda-mapper" % "0.2.1"
    ,"postgresql" % "postgresql" % "9.1-901.jdbc4"
    ,"mysql" % "mysql-connector-java" % "5.1.18"
  )

def initFilecontent(id: String, desc: String) = format(
"""#!/bin/bash
# /etc/init.d/%1$s
# debian-compatible %1$s startup script.
# Original version by: Amelia A Lewis <alewis@ibco.com>
# updates and tweaks by: Rodolfo Hansen <rhansen@kitsd.com>
#
### BEGIN INIT INFO
# Provides:          %1$s
# Required-Start:    $remote_fs $syslog $network
# Required-Stop:     $remote_fs $syslog $network
# Default-Start:     2 3 4 5
# Default-Stop:      0 1 6
# Short-Description: Start %1$s at boot time
# Description:       Controls the %1$s Play! Framework standalone application.
### END INIT INFO

PATH=/bin:/usr/bin:/sbin:/usr/sbin

DESC="%2$s"
NAME=%1$s
USER=%1$s
SCRIPTNAME=/etc/init.d/$NAME
PIDFILE=/var/run/%1$s.pid
LOGFILE=/var/log/%1$s/console.log

[ -r /etc/default/$NAME ] && . /etc/default/$NAME

DAEMON=/usr/bin/daemon
DAEMON_ARGS="--name=$NAME --inherit --output=$LOGFILE --pidfile=$PIDFILE"

SU=/bin/su

# Exit if the package is not installed
[ -x "$DAEMON" ] || (echo "daemon package not installed" && exit 0)

# load environments
if [ -r /etc/default/locale ]; then
  . /etc/default/locale
  export LANG LANGUAGE
elif [ -r /etc/environment ]; then
  . /etc/environment
  export LANG LANGUAGE
fi

# Load the VERBOSE setting and other rcS variables
. /lib/init/vars.sh

# Define LSB log_* functions.
# Depend on lsb-base (>= 3.0-6) to ensure that this file is present.
. /lib/lsb/init-functions

# Make sure we run as root, since setting the max open files through
# ulimit requires root access
if [ `id -u` -ne 0 ]; then
    echo "The $NAME init script can only be run as root"
    exit 1
fi


check_tcp_port() {
    local service=$1
    local assigned=$2
    local default=$3

    if [ -n "$assigned" ]; then
        port=$assigned
    else
        port=$default
    fi

    count=`netstat --listen --numeric-ports | grep \:$port[[:space:]] | grep -c . `

    if [ $count -ne 0 ]; then
        echo "The selected $service port ($port) seems to be in use by another program "
        echo "Please select another port to use for $NAME"
        return 1
    fi
}

#
# Function that starts the daemon/service
#
do_start()
{
    # the default location is /var/run/%1$s.pid but the parent directory needs to be created
    mkdir `dirname $PIDFILE` > /dev/null 2>&1 || true
    chown $USER `dirname $PIDFILE`
    # Return
    #   0 if daemon has been started
    #   1 if daemon was already running
    #   2 if daemon could not be started
    $DAEMON $DAEMON_ARGS --running && return 1

    # Verify that the jenkins port is not already in use, winstone does not exit
    # even for BindException
    check_tcp_port "http" "$HTTP_PORT" "9000" || return 1

    # If the var MAXOPENFILES is enabled in /etc/default/%1$s then set the max open files to the
    # proper value
    if [ -n "$MAXOPENFILES" ]; then
        [ "$VERBOSE" != no ] && echo Setting up max open files limit to $MAXOPENFILES
        ulimit -n $MAXOPENFILES
    fi

    # --user in daemon doesn't prepare environment variables like HOME, USER, LOGNAME or USERNAME,
    # so we let su do so for us now
    $SU -l $USER --shell=/bin/bash -c "$DAEMON $DAEMON_ARGS -- /var/lib/%1$s/start $PLAY_ARGS" || return 2
}


#
# Verify that all jenkins processes have been shutdown
# and if not, then do killall for them
#
get_running()
{
    return `ps -U $USER --no-headers -f | egrep -e '(java|daemon)' | grep -c . `
}

force_stop()
{
    get_running
    if [ $? -ne 0 ]; then
        killall -u $USER java daemon || return 3
    fi
}

# Get the status of the daemon process
get_daemon_status()
{
    $DAEMON $DAEMON_ARGS --running || return 1
}


#
# Function that stops the daemon/service
#
do_stop()
{
    # Return
    #   0 if daemon has been stopped
    #   1 if daemon was already stopped
    #   2 if daemon could not be stopped
    #   other if a failure occurred
    get_daemon_status
    case "$?" in
  0)
      $DAEMON $DAEMON_ARGS --stop || return 2
        # wait for the process to really terminate
        for n in 1 2 3 4 5 6 7 8 9 10 11 12 13 14 15 16 17 18 19 20; do
            sleep 1
            $DAEMON $DAEMON_ARGS --running || break
        done
        if get_daemon_status; then
          force_stop || return 3
        fi
      ;;
  *)
      force_stop || return 3
      ;;
    esac

    # Many daemons don't delete their pidfiles when they exit.
    rm -f $PIDFILE
    return 0
}

case "$1" in
  start)
    log_daemon_msg "Starting $DESC" "$NAME"
    do_start
    case "$?" in
        0|1) log_end_msg 0 ;;
        2) log_end_msg 1 ;;
    esac
    ;;
  stop)
    log_daemon_msg "Stopping $DESC" "$NAME"
    do_stop
    case "$?" in
        0|1) log_end_msg 0 ;;
        2) log_end_msg 1 ;;
    esac
    ;;
  restart|force-reload)
    #
    # If the "reload" option is implemented then remove the
    # 'force-reload' alias
    #
    log_daemon_msg "Restarting $DESC" "$NAME"
    do_stop
    case "$?" in
      0|1)
        do_start
        case "$?" in
          0) log_end_msg 0 ;;
          1) log_end_msg 1 ;; # Old process is still running
          *) log_end_msg 1 ;; # Failed to start
        esac
        ;;
      *)
    # Failed to stop
  log_end_msg 1
  ;;
    esac
    ;;
  status)
  get_daemon_status
  case "$?" in
   0)
    echo "$DESC is running with the pid `cat $PIDFILE`"
    rc=0
    ;;
  *)
    get_running
    procs=$?
    if [ $procs -eq 0 ]; then
      echo -n "$DESC is not running"
      if [ -f $PIDFILE ]; then
        echo ", but the pidfile ($PIDFILE) still exists"
        rc=1
      else
        echo
        rc=3
      fi

    else
      echo "$procs instances of jenkins are running at the moment"
      echo "but the pidfile $PIDFILE is missing"
      rc=0
    fi

    exit $rc
    ;;
  esac
  ;;
  *)
    echo "Usage: $SCRIPTNAME {start|stop|status|restart|force-reload}" >&2
    exit 3
    ;;
esac

exit 0
""", id, desc)

  val main = play.Project(appName, appVersion, appDependencies).settings(
	  oneJarSettings ++
	  linuxSettings ++
	  rpmSettings ++
	  makeDeploymentSettings(Rpm, packageBin in Rpm, "rpm") ++ 
	  Seq(
		name in Linux := "moat-web",
	packageSummary := "TODO",
	packageDescription := " A descriptioin of your project",
	version in Rpm <<= version,
	rpmRelease := "1",
	rpmVendor := "iatha",
	maintainer := "Ian W. Atha <ian+moat@atha.io>",
	linuxPackageMappings <++= (baseDirectory, target, normalizedName, packageSummary, PlayProject.playPackageEverything, dependencyClasspath in Runtime, oneJar in Linux) map {
		(root, target, name, desc, pkgs, deps, onejar) ⇒
			val usr = "iatha"
			val grp = "iatha"
			val start = target / "start"
			val config = Option(System.getProperty("config.file"))
			def startFileContent = format(
			"""#!/usr/bin/env sh

			exec java $* -cp "`dirname $0`/lib/*" %s play.core.server.NettyServer `dirname $0` $@
			""", config.map(_ ⇒ "-Dconfig.file=`dirname $0`/application.conf ").getOrElse(""))
			val init = target / "initFile"
			IO.write(start, startFileContent)
			IO.write(init, initFilecontent(name, desc))
			pkgs.map { pkg ⇒
				packageMapping(pkg -> format("/var/lib/%s/%s", name, pkg.getName)) withUser(usr) withGroup(grp)
			} ++
			deps.filter(_.data.ext == "jar").map { dependency ⇒
				val depFilename = dependency.metadata.get(AttributeKey[ModuleID]("module-id")).map { module ⇒
					module.organization + "." + module.name + "-" + module.revision + ".jar"
				}.getOrElse(dependency.data.getName)
				packageMapping(dependency.data -> format("/var/lib/%s/lib/%s", name, depFilename)) withUser(usr) withGroup(grp) withPerms("0644")
			} ++
			(config map { cfg ⇒
				packageMapping(root / cfg -> format("/var/lib/%s/application.conf", name)) withUser(usr) withGroup(grp) withPerms("0644")
	        }) ++ Seq(
				packageMapping(start -> format("/var/lib/%s/start", name)) withUser(usr) withGroup(grp),
	            packageMapping(init -> format("/etc/init.d/%s/", name)) withConfig(),
				packageMapping(root / "README" -> format("/var/lib/%s/README", name)) withUser(usr) withGroup(grp) withPerms("0644"),
				packageMapping(onejar -> ("/tmp/test.jar"))
			)
		}
	):_*
)
}
