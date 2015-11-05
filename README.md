# keenhome.device-type.smartvents

Authors:             Keen Home, Yves Racine

linkedIn profile:   ca.linkedin.com/pub/yves-racine-m-sc-a/0/406/4b/

Date:               2014-03-31


<br/> [![PayPal](https://www.paypalobjects.com/en_US/i/btn/btn_donate_SM.gif)](
https://www.paypal.com/cgi-bin/webscr?cmd=_donations&business=yracine%40yahoo%2ecom&lc=US&item_name=Maisons%20ecomatiq&no_note=0&currency_code=USD&bn=PP%2dDonationsBF%3abtn_donateCC_LG%2egif%3aNonHostedGuest)

**************************************************************************************************

This device type's configure tile changes the pressure and temp's refresh rate to be 5-10 minutes instead of 1 minute (set by default) in order to avoid excessive battery usage.

Setup time: about 2 minutes

=====================
INSTALLATION STEPS
=====================

/*********************************************************************************************

<b>1) Create the device type (Keen Home Smart Vent)</b>
/*********************************************************************************************

a) Go to https://graph.api.smartthings.com/ide/devices

b) Hit the "+New Device Type" at the top right corner

c) Hit the "From Code" tab on the left corner

d) Copy and paste the code from 

http://github.com/yracine/keenhome.device-type/blob/master/smartvents.groovy

e) Hit the create button at the bottom

f) Hit the "publish/for me" button at the top right corner (in the code window)

/*********************************************************************************************

<b>2) Assign your devices to the new device type (should be at the end of your device list)</b>
/*********************************************************************************************

a) Go to https://graph.api.smartthings.com/device/list

b) Click on your device(s), and then edit at the bottom

c) Choose the new device type (v1.0.1) from the list using the type field

d) Hit the save button at the bottom

/*********************************************************************************************

<b>3) Configure the new device settings in the smartThing app</b>
/*********************************************************************************************

a) Go to the Home tab, click on Things, and the right device

b) Hit the configure tile to set the new 'refresh rate' settings.
