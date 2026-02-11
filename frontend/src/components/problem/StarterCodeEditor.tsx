import { useState } from 'react';
import { CodeEditor } from '@/components/editor/CodeEditor';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { Button } from '@/components/ui/button';
import { SUPPORTED_LANGUAGES } from '@/types';
import { RotateCcw } from 'lucide-react';

interface StarterCodeEditorProps {
  value: Record<string, string>;
  onChange: (starterCode: Record<string, string>) => void;
}

export function StarterCodeEditor({ value, onChange }: StarterCodeEditorProps) {
  const [activeTab, setActiveTab] = useState(String(SUPPORTED_LANGUAGES[0].id));

  const handleCodeChange = (languageId: string, code: string) => {
    onChange({
      ...value,
      [languageId]: code,
    });
  };

  const handleResetToDefault = (languageId: string) => {
    const defaultCode = getDefaultTemplate(Number(languageId));
    onChange({
      ...value,
      [languageId]: defaultCode,
    });
  };

  const getCurrentCode = (languageId: string): string => {
    return value[languageId] ?? getDefaultTemplate(Number(languageId));
  };

  return (
    <div className="space-y-2">
      <h3 className="font-semibold">Starter Code Templates</h3>
      <p className="text-sm text-muted-foreground">
        Provide starter code templates for each language. Users will see this when they start solving.
      </p>
      <Tabs value={activeTab} onValueChange={setActiveTab}>
        <TabsList>
          {SUPPORTED_LANGUAGES.map((lang) => (
            <TabsTrigger key={lang.id} value={String(lang.id)}>
              {lang.name}
            </TabsTrigger>
          ))}
        </TabsList>
        {SUPPORTED_LANGUAGES.map((lang) => (
          <TabsContent key={lang.id} value={String(lang.id)} className="mt-4 space-y-2">
            <div className="flex justify-end">
              <Button
                variant="outline"
                size="sm"
                onClick={() => handleResetToDefault(String(lang.id))}
                data-testid={`reset-${lang.id}`}
              >
                <RotateCcw className="h-4 w-4 mr-1" />
                Reset to Default
              </Button>
            </div>
            <div className="h-[300px] border rounded-md overflow-hidden">
              <CodeEditor
                value={getCurrentCode(String(lang.id))}
                onChange={(code) => handleCodeChange(String(lang.id), code)}
                language={lang.monacoLanguage}
              />
            </div>
          </TabsContent>
        ))}
      </Tabs>
    </div>
  );
}

export function getDefaultTemplate(languageId: number): string {
  switch (languageId) {
    case 62: // Java
      return `import java.util.*;

public class Solution {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
    }
}`;
    case 71: // Python
      return `# Your code here
`;
    case 54: // C++
      return `#include <iostream>
#include <vector>
using namespace std;

int main() {
    return 0;
}`;
    default:
      return '';
  }
}
