# Notes

here are the things we have figured out during development

- Hot reload is evil and don't trust it
- Do not use a mutable list for compose remembered state
- to get ~~vertical divider~~ anything to render you must in the row they are in use the modifiers:

```kt
modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min)
```