### Information

The ONE is a simulation environment that is capable of

* generating node movement using different movement models
* routing messages between nodes with various DTN routing algorithms and sender and receiver types
* visualizing both mobility and message passing in real time in its graphical user interface.
* ONE can import mobility data from real-world traces or other mobility generators. It can also produce a variety of reports from node movement to message passing and general statistics.

The ONE simulator was developed at Aalto University and is now maintained and extended in cooperation between [Aalto University (Comnet)](http://comnet.aalto.fi/en/) and [Technische Universität München (Connected Mobility)](http://www.cm.in.tum.de/index.php?id=5).

Developers: [Ari Keränen](https://twitter.com/ari_kk), Teemu Kärkkäinen, Mikko Pitkänen, Frans Ekman, Jouni Karvo, and [Jörg Ott](https://www.cm.in.tum.de/index.php?id=16) 

### Acknowledgments

The ONE simulator has been developed in the SINDTN and CATDTN projects supported by Nokia Research Center (Finland), in the TEKES ICT-SHOK Future Internet and IoT-SHOK projects, Academy of Finland projects RESMAN and Picking Digital Pockets (PDP), European Community's Seventh Framework Programme SCAMPI project, and supported by EIT ICT Labs.


### Releases

The latest release:
* [The ONE v1.6.0](https://github.com/akeranen/the-one/tree/v1.6.0)

Previous releases:
* [The ONE v.1.5.1-RC2](https://www.netlab.tkk.fi/tutkimus/dtn/theone/down/one_1.5.1-RC2.zip)
* [The ONE v.1.4.1](https://www.netlab.tkk.fi/tutkimus/dtn/theone/down/one_1.4.1.zip)

Older versions of the ONE are available from the [old ONE homepage](https://www.netlab.tkk.fi/tutkimus/dtn/theone/).

### Referring to the ONE simulator

If you have used the ONE simulator in your research, please use the SIMUTools paper ([[PDF]](https://www.netlab.tkk.fi/tutkimus/dtn/theone/pub/the_one_simutools.pdf) [[BibTeX]](https://www.netlab.tkk.fi/tutkimus/dtn/theone/pub/theone_bib.txt)) as the reference.


### Community Resources

The users of the ONE simulator have created many helpful resources:

* [Connection Trace Analysis tool by Juliano Fischer Naves](https://github.com/julianofischer/traceanalysis)
* [Delay-Tolerant Blogging by Juliano Fischer Naves](http://www.delaytolerantnetworks.com/)
* [Barun Saha's Knowledge Base for the ONE](https://theonekb-barunsaha.rhcloud.com/)
* [The ONE Simulator for Beginners](http://one-simuator-for-beginners.blogspot.in/2013/08/one-simulator-introduction.html)
* [ONE Tutorial by Mauro Margalho Coutinho](http://www.margalho.pro.br/subsites/theone.html)
* [Agussalim's metrics compilation](http://agoes.web.id/metric-description-from-simulator/)
* [Barun Saha's blog on Delay-tolerant Networks](http://delay-tolerant-networks.blogspot.com.br/)
* [Sahil Gupta's web site related to DTN and ONE](https://sites.google.com/site/sahilgupta221231/file-cabinet)


### Contact

The ONE mailing list has been closed. For questions on use of the ONE we recommend [Stack Overflow](https://stackoverflow.com/questions/tagged/dtn).

### License

The program is released under GPLv3 license. Copyrights of the included map data of Helsinki downtown are owned by Maanmittauslaitos.


### Running (a really quick help)

Download the program. Compile it using your favourite IDE or `compile.bat` (on Windows) or `compile.sh` (on Linux or Mac OS X).

Start a simulation by typing in (Linux/Unix/OS X) terminal:

    ./one.sh

or (in Windows):

    one.bat

After a short while, the GUI should start and a simulation based on default settings should be up and running.

Every simulation run uses the settings from default_settings.txt, if one exists. You can give an additional configuration file(s) as a parameters to define new settings or override the ones defined in default settings. For example:

    ./one.sh example_settings/snw_settings.txt

That would use the (included) snw_settings.txt settings configuration file and change the router for all nodes to Spray and Wait (default is "passive router", i.e., no routing logic). See default_settings.txt for information about the different settings.

If you don't wish to use the GUI, you can run simulations in batch mode using -b option. In batch mode you can also define the number of runs using different run indexes. For example:

    ./one.sh -b 22 example_settings/snw_comparison_settings.txt 

That would run Spray and Wait comparison using 11 different message copy counts and with binary and normal mode. See Settings class' javadoc for details about run indexing.

To create a local copy of the javadoc documentation, run the `create_docs.sh` script in the doc folder.

See the project [README](https://github.com/akeranen/the-one/wiki/README) for more information.


### Publications

Michael Solomon Desta, Ari Keränen, Teemu Kärkkäinen, Esa Hyytiä, Jörg Ott: Evaluating (Geo) Content Sharing with the ONE Simulator. Invited demo paper. Proceedings of the 14th ACM Symposium Modeling, Analysis and Simulation of Wireless and Mobile Systems (MSWiM), November 2013. [[PDF]](http://www.netlab.tkk.fi/~jo/papers/2013-11-mswim-one-15.pdf)

Ari Keränen, Teemu Kärkkäinen, and Jörg Ott: Simulating Mobility and DTNs with the ONE (Invited paper). Journal of Communications, Academy Publisher, Vol 5 No 2, pp 92-105, February 2010. 

Ari Keränen, Jörg Ott and Teemu Kärkkäinen: The ONE Simulator for DTN Protocol Evaluation. SIMUTools'09: 2nd International Conference on Simulation Tools and Techniques. Rome, March 2009. [[PDF]](https://www.netlab.tkk.fi/tutkimus/dtn/theone/pub/the_one_simutools.pdf) [[BibTeX]](https://www.netlab.tkk.fi/tutkimus/dtn/theone/pub/theone_bib.txt)

Jouni Karvo and Jörg Ott: Time Scales and Delay-Tolerant Routing Protocols. Proceedings of the ACM MobiCom CHANTS Workshop, September 2008. San Francisco, September 2008. [[PDF]](http://www.netlab.hut.fi/~jo/papers/2008-09-chants-timescales.pdf) [[BibTeX]](https://www.netlab.tkk.fi/tutkimus/dtn/theone/pub/timescales_bib.txt)

Frans Ekman, Ari Keränen, Jouni Karvo, and Jörg Ott: Working Day Movement Model. 1st SIGMOBILE Workshop on Mobility Models for Networking Research, Hong Kong, May 2008. [[PDF]](http://www.netlab.hut.fi/tutkimus/distance/papers/2008-mobmod-working-day-model.pdf) [[BibTeX]](https://www.netlab.tkk.fi/tutkimus/dtn/theone/pub/wdm_bib.txt)

Ari Keränen: Opportunistic Network Environment simulator. Special Assignment, Helsinki University of Technology, Department of Communications and Networking, May 2008. [[PDF]](https://www.netlab.tkk.fi/tutkimus/dtn/theone/pub/the_one.pdf)

Ari Keränen and Jörg Ott: Increasing Reality for DTN Protocol Simulations. Technical Report, Helsinki University of Technology, Networking Laboratory, July 2007. [[PDF]](http://www.netlab.tkk.fi/~jo/papers/2007-ONE-DTN-mobility-simulator.pdf) 
