/*imatveev13@nm.ru
 * outputs meitrack login packet then  position data packet
 * ./dummy | nc localhost [meitrack OpenGTS server port]
 * */
 
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
//#include <mem.h>

void send_hexstr(char * hexstr){
    unsigned int chr;
    char substr[3];
    int q;
    
    for(q=0; q < strlen(hexstr); q = q + 2){
        bzero(substr, 3);
        strncpy(substr, hexstr + q, 2);
    
        chr = strtol(substr,0,16);
    
        printf( "%c", chr);
    }

}

int main(){

char * hexstr_login = "24240011123456FFFFFFFF50008B9B0D0A";
//char * hexstr = "24240054085716520874FF99553039323433322E3030302C412C303631342E323430342C532C31303635382E333331322C452C302E30302C2C3037313031302C2C2A30317C302E387C34327C303030309DB6";
//char * hexstr = "24240054085716520874FF99553039323433322E3030302C412C303631342E323430342C532C31303635382E333331322C452C302E30302C2C3037313031302C2C2A30317C302E387C34327C303030309DB60D0A";
char * hexstr = "2424005485716520874FFF99553039323732312E3030302C412C303631342E323432322C532C31303635382E333333372C452C302E30302C2C3231313031302C2C2A30377C302E377C34367C303030308C180D0A";
send_hexstr(hexstr_login);
sleep(1);
send_hexstr(hexstr);

}


