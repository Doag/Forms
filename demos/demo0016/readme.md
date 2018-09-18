Demo 0016

Friedhold Matz - fmatz.com@gmail.com

Twitter: @FriedholdMatz


# Forms AutoComplete ComboBox

The first version of a quasi Oracle Forms Auto Complete Combo Box with native PL/SQL Forms code was developed 2007,
http://friedhold-matz.blogspot.de/2007/09/autocomplete-combobox.html
http://www.free-dev.com/chk_lboxproto2.htm
here is the adapted PoC Demo version for Oracle Forms 12c.

## Getting Started

<img src="http://www.fmatz.com/AutoCB-28-01-_2018_18-20-42.png" />

### Prerequisites

- Oracle Forms 12.2.1.3

### Setup and Deployment

- Download 3 files: OLB, FMB and SQL

#### Application setup

    1.  Create the table eurocities and insert data in your test schema:
        SQL> @cr_eurocities.sql
    2.  Create a new Form
    3.  Add object group from cbox3.olb per drag and copy
    4.  Insert in the WHEN-NEW-FORMS-INSTANCE trigger
```
        pkg_CBOX.populate_auto_cbox('CBOX',
            'select name,name from europecities order by 1');
```

### Programming

## Running the tests

- Compile and deploy the Form.

Here the demo: 

<img src="http://www.fmatz.com/AutoCBox.gif" />

## Known issues

## Not implemented
