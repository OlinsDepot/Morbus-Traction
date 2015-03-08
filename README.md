Morbus-Traction
===============

This repository supports development of an Android app to control a model
railroad layout through a Morbus server.

MMorBus is a digital interconnect scheme for model railroads defined by
Olin's Depot.  The name MorBus stands for Model Railroad Bus.  The purpose
of the MorBus is to connect all actuators and sensors of a model railroad
that are fixed (not rolling stock).  DCC is used to communicate with rolling
stock, with the DCC generators themselves being controlled from the MorBus.
The MorBus therefore provides a single computer interface to an entire model
railroad layout. 

The MorBus itself is physically a CAN bus, however, it can be useful to
communicate MorBus frames over a bi-directional byte stream such as USB or
TCP. The Morbus server allows a PC or handheld device to send and receive
Morbus frames, in this case, through a TCP connection.

This project then, is building an application to run on Android devices that
presents an interface to the user in a form of throttles, and switches, and
translates user actions on this interface into Morbus commands through the
server. Similarly the interface has indicators that display status representing
events on the layout reported by the server.

A goal of this project is that the resulting system should be as "plug-and-play"
as possible.
