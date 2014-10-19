package nu.thiele.kuskemaapp.ui;

public interface OnManualCourseAdditionRequest{
	public void manualCourseAdditionRequest();
	public void manualCourseAdditionRequest(String courseName);
	public void manualCourseAdditionRequest(int day);
	public void manualCourseAdditionRequest(int day, String courseName);
}
