# Android Java Application Demo

Including functions below:

> ．Check Currency  
> ．QR Code Scanner <- submenu [Generate QR Code, Read QR Code from image]  
> ．Jot Notes

## Check Currency:

．Using Free API: https://tw.rter.info/capi.php  
-remark: API will update every 15 minutes  
．Volley is used for get async json call back because AsyncTask was deprecated  
．Auto detect user location(network location/gps location) for faster currency checking;  
use locale otherwise

> consequence when using different locale  
> ^ e.g. in Traditional Chinese(Hong Kong) using English(USA) will use USD as default currency instead of HKD

-full list  
．Quick search filtering when using currency code/currency name as input  
．Reactive amount update triggered by the input  
．Can go into advance page when click on the currency in the list  
e.g. clicked | New Taiwan Dollar |  
-advance page  
．Filter using currency code/currency name or clicking GridView item for adding a row  
．Multiple row(Max 8) with input field for different currency convert to selected currency before  
e.g.

<pre>
| HKD | 5000.5 X |  
| USD | 2000   X |  
| GBP | 1000   X |  
= TWD $112,175.235  
</pre>

．Decimal supported and have input validation for invalid/empty input  
．press X button will delete te row, amount total will update also

> Working on long press clear input field

．Sub total display as the hint above the input field when "CAL!" button pressed

## QR Code Scanner:

．Using ZXING: https://github.com/zxing/zxing  
．Instant open webview or text depends on QR Code source(text/url)  
．Long press for submenu  
-generate QR Code  
．Input text/url to generate QR Code reactively  
-Read QR Code from image  
．Perform action like QR Code Scanner when the image contain QR Code

> Working on add to clipboard function

## Jot Notes:

．Using MongoDB:  
．https://www.mongodb.com/  
．https://www.mongodb.com/realm  
．Text is supported, UTF-8(e.g. emoji, unicode characters etc.) supported by MongoDB  
．Drawing(Bitmap) is supported, will converted to string before upload to DB  
．ALL notes are stored in MongoDB and can peform actions after: insert/update/delete  
．Single short click Jot Notes will create a new Text note, can switch to Drawing by pressing :art: button at the bottom right;  
．Drawing can switch to Text note by pressing :memo: button at the bottom right  
．More option button: :arrow_up_small: Drawing can peform clean all; change color by color picker(dialog); change stroke style(menu), stroke width(slider) etc. with this.

> Working on Text note more option, like checkbox task(Map<Boolean, String> task, for intuitive :ballot_box_with_check: and update in grid);  
> ^e.g.

## <pre>

## Title1

line1line1line1
▢ TODO abc
line2line2line2
line3line3line3
✓ FINISHED 123
line4line4line4

---

    |Submit|

</pre>

> adding Tag for note(no matter Text/Drawing) for a better search filter action in the grid view

．Long press Jot Notes in main menu for retrieving all data from db and listed as grid for preview  
．Click the element in gridview will go into designated edit view, depends on which Text/Drawing is clicked  
．Press upload button at the bottom will update/insert the data, depends on Edit/New note  
．Long pressed an element in the gridview will popup menu for DELETE a note, delete on db

> Working on alarm function, details below:  
> ^ will sync with my telegram bot wrote in python(with command /syncnotealarms in tgbot) to perform cross platform/device notification alarm  
> ^ Pros: do not need to intall this apk in every devices  
> ^ Cons: self telegram bot is needed; telegram installed

> ~~Working on~~ ✓DONE security of the notes, add isLocked indicator for grand read/write/delete permission after fingerprint validation if set in the note(in More option button: :arrow_up_small:)  
> ^ More Info : ✓DONE not only fingerprint authentication, but also pin password for note added: both method can use together(textnote for now, drawing note coming soon)  
> ^ More Info+: ~~trying to~~ ✓DONE implement blur image for locked Drawing note, text note content will be replace will \*
