package nu.thiele.kuskema.ui;

public class Time implements Comparable<Time>{
    	public String kursus;
		public String tid;
		public String type;
    	public Time(String tid, String kursus, String type){
    		this.kursus = kursus;
    		this.tid = tid;
    		this.type = type;
    	}
    	
		@Override
		public int compareTo(Time arg0) {
			try{
				if(arg0 == null || this == null) return 1;
				String[] thissplit = this.tid.split("-");
				String[] thatsplit = arg0.tid.split("-");
				if(thissplit[0].equals(tid) || thatsplit[0].equals(arg0.tid)) return 1;
				
				int thisstart = Integer.valueOf(thissplit[0].split(":")[0])*60+Integer.valueOf(thissplit[0].split(":")[1]);
				int thatstart = Integer.valueOf(thatsplit[0].split(":")[0])*60;
				if(thisstart < thatstart) return -1;
				else if(thisstart > thatstart) return 1;
				int thisslut = Integer.valueOf(thissplit[1].split(":")[0])*60+Integer.valueOf(thissplit[1].split(":")[1]);
				int thatslut = Integer.valueOf(thatsplit[1].split(":")[0])*60+Integer.valueOf(thatsplit[1].split(":")[1]);
				if(thisslut < thatslut) return -1;
				else if(thisslut > thatslut) return 1;
				return 0;
			}
			catch(Exception e){
				return 0;
			}
		}    	
		
		public String toString(){
			return this.kursus+": "+this.type+", kl. "+this.tid;
		}
    }