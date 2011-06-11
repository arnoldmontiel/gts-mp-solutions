imatveev13@nm.ru(AKA monicar) 2010/10/20

This is a very basic OpenGTS server for MeiTrack trackers: 
GT30, GT30X, GT60, VT300, VT310.

See GPRS_Communication_Protocol-V1.45.pdf

The server only parses 
0x5000 Login packet
and
0x9955 Position Report packet.

The server wasn't tested with a tracker, I don't have one.

Installation:

copy meitrack direcrory to .../OpenGTS/src/org/opengts/servers/

-----------In build.xml 

--edit:

<target name="compile.servers" depends="compile.base,gtsdmtp,icare,aspicore,sipgear,template,monicar,meitrack" 

--add:

	  <!-- Target: Device Parser Module example meitrack-->
	  <target name="meitrack" depends="prepare,gtsdb" 
	    description="Create 'Device Communication Server' meitrack ...">
	    <echo message="meitrack ..."/>

	    <!-- compile meitrack -->
	    <javac srcdir="${src.gts}"
	        includeAntRuntime="false"
	        source="${compiler.source}"
	        target="${compiler.target}"
	        destdir="${build.home}"
	        debug="${compile.debug}"
	        nowarn="${compile.nowarn}"
	        deprecation="${compile.deprecation}"
	        optimize="${compile.optimize}">
	        <compilerarg compiler="${compiler.compiler}" value="${compile.Xlint}"/>
	        <classpath refid="compile.classpath"/>
	        <include name="org/opengts/servers/*.java"/>
	        <include name="org/opengts/servers/meitrack/**/*.java"/>
	    </javac>

	    <!-- create monicar.jar server -->
	    <jar jarfile="${build.lib}/meitrack.jar">
	        <manifest>
	            <attribute name="Class-Path" value="${Server_Jar_Classpath}"/>
	            <attribute name="Main-Class" value="org.opengts.servers.meitrack.Main"/>
	        </manifest>
	        <fileset dir="${build.home}">
	            <include name="org/opengts/servers/*.class"/>
	            <include name="org/opengts/servers/meitrack/**/*.class"/>
	        </fileset>
	    </jar>

	  </target>

-----------In dcservers.xml

--add:

    <DCServer name="meitrack">
        <Description><![CDATA[
            MeiTrack Server
            ]]></Description>
            <!--
        <UniqueIDPrefix><![CDATA[
            template_
            imei_
            *
            ]]></UniqueIDPrefix>
            -->
        <ListenPorts 
            tcpPort="30005" 
            />
        <Properties>
        </Properties>
    </DCServer>

--edit tcpPort to your liking.


compile the server
ant meitrack

run the server
bin/runserver.pl  -s meitrack -i

Tell the results on the OpenGTS forum: 
https://sourceforge.net/projects/opengts/forums/forum/579835
