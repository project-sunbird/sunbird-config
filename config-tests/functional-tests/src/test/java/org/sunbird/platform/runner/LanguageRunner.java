/**
 * 
 */
package org.sunbird.platform.runner;

import org.sunbird.platform.language.DictionaryAPITests;
import org.sunbird.platform.language.LessonComplexityTestCases;
import org.sunbird.platform.language.LinkWordsTestCases;
import org.sunbird.platform.language.ToolsAPITests;
import org.sunbird.platform.language.WordPatchUpdateTest;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * @author gauraw
 *
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({DictionaryAPITests.class, LessonComplexityTestCases.class, LinkWordsTestCases.class, ToolsAPITests.class, WordPatchUpdateTest.class})
public class LanguageRunner {

}
